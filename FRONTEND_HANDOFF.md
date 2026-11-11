# Passage de relais frontend — Frogger

**L'équipe interface est propriétaire du frontend web.** À la demande explicite de l'utilisateur, les modifications visuelles et web du présent chantier ont été arrêtées pendant la reprise de l'interface. Les fichiers présents sont conservés ; ils ne constituent pas une interface terminée ni validée au navigateur. Aucune publication n'a été effectuée dans ce chantier.

## Fichiers présents au passage de relais

Créés avant l'arrêt : `web/index.html`, `web/src/style.css`, `web/public/frog.svg`, `web/package.json`, `web/package-lock.json`, `web/vite.config.js`. Les polices Manrope et DM Sans sont installées localement via Fontsource ; leurs licences OFL devront accompagner la distribution si elles sont retenues.

**Point d'entrée manquant :** `web/index.html` référence `web/src/main.js`, qui n'était pas encore créé. Aucun renderer Canvas ni contrôleur DOM web n'a été écrit. L'équipe interface peut reprendre ou remplacer cette proposition visuelle, selon ses consignes. Le SVG de grenouille est un dessin original, sans asset externe.

Codex conserve la responsabilité du moteur Java, de Swing, de la compilation TeaVM, des tests de règles et de parité, et de la documentation technique. `web/src/generated/engine.js` est un résultat de compilation ignoré par Git : ne pas le modifier à la main.

## API stable du moteur

Compilation : `./mvnw -Pweb verify` ou `mvn -Pweb verify`. Elle génère un ES module importable dans Vite :

```js
import { start, command, advance, snapshot } from './generated/engine.js';

start('CLASSIC', 2021); // difficulté WALK | CLASSIC | EXPRESS ; graine entière signée 32 bits
command('up');         // up | right | down | left | pause | resume | continue | menu
advance(20);           // temps entier en ms, entre 0 et 60 000
const game = JSON.parse(snapshot());
```

Les fonctions d'action retournent `undefined`. `snapshot()` retourne du JSON, pas un objet JavaScript. L'instance est unique au module. Aucun code Swing/AWT n'est inclus dans le module web. **Les règles ne doivent pas être réimplémentées en JavaScript.**

`start` réinitialise la partie entière, les vies, le score et les refuges. Depuis une nouvelle session, une graine et une séquence d'actions identiques reproduisent le même état. Le compteur de messages continue d'augmenter entre deux parties dans la même session. `menu` abandonne l'état jouable pour revenir au menu ; afficher une confirmation avant l'appel si une partie est engagée. Les commandes inconnues ou temps hors contrat lèvent une erreur.

## États et actions

| État | Sens | Action principale |
| --- | --- | --- |
| `MENU` | Aperçu du terrain, jeu arrêté | `start(mode, seed)` |
| `PLAYING` | Temps, trafic et commandes actifs | `command(direction)` ou `pause` |
| `PAUSED` | Tout est figé | `resume` ou `continue` |
| `HIT` | Une vie perdue ; cause dans `event` | `continue`, départ d'une nouvelle traversée |
| `HOME` | Refuge atteint, partie figée | `continue`, nouvelle traversée |
| `LEVEL_CLEAR` | Trois refuges remplis | `continue`, étape suivante |
| `WON` | Neuf sauvetages et trois étapes terminées | `start(mode, seed)` |
| `LOST` | Aucune vie restante | `start(mode, seed)` |

Le moteur bloque les mouvements hors `PLAYING` et impose 120 ms entre deux sauts réussis. Les touches ne font jamais avancer le trafic. `advance` n'a aucun effet hors `PLAYING`. Une seule vie est retirée par accident. Les états `HIT`, `HOME` et `LEVEL_CLEAR` attendent une action explicite ; aucun redémarrage automatique.

## Snapshot version 1

```js
{
  version: 1,
  state: 'PLAYING', difficulty: 'CLASSIC', seed: 2021,
  level: 1, score: 0, lives: 3, saved: 0,
  remainingMs: 55000, timeLimitMs: 55000, elapsedMs: 0,
  frog: { x: 6.5, y: 12, direction: 'UP' },
  homes: [false, false, false],
  event: { id: 1, type: 'START', message: 'Rejoignez un refuge libre, tout en haut.' },
  lanes: [
    { row: 11, type: 'ROAD', speed: 0.9, length: 1.35, positions: [/* nombres */] }
  ]
}
```

- Terrain : **13 colonnes × 13 rangées**, origine en haut à gauche. `frog.x` est le **centre horizontal** en cases ; `frog.y` est l'indice de rangée. Dessiner son centre à `(x, y + 0.5)`. Rayon de collision routière : 0,28 case.
- Rangées routières : 8, 9, 10, 11. Rivières : 1, 2, 4, 5, 6. Berges sûres : 3, 7, 12. Refuges en rangée 0, centres **2,5 ; 6,5 ; 10,5**. La position finale est ajustée au centre du refuge.
- `positions` contient le **bord gauche** de chaque véhicule/tronc visible, éventuellement négatif ; sa largeur est `length` cases. Clippez le dessin aux bords du terrain.
- `speed` est en cases/seconde, positive vers la droite. Le moteur transporte automatiquement la grenouille sur les troncs. Ne pas ajouter une seconde dérive côté renderer.
- L'eau exige un centre sur un tronc avec une marge intérieure de 0,1 case. Le moteur traite déjà les sorties latérales, les collisions et les refuges remplis.
- `homes` représente les refuges de l'étape actuelle ; `saved` le total sur 9. Lors d'une arrivée, le renderer peut masquer la grenouille active et dessiner celles déjà à l'abri.
- `event.id` augmente pour chaque message significatif. Mettre à jour une région `aria-live=polite` une fois par nouvel identifiant, sans annoncer chaque frame ni chaque seconde.
- Causes d'accident : `CAR`, `WATER`, `EDGE`, `TIME`. Autres événements : `READY`, `START`, `MENU`, `PAUSE`, `RESUME`, `CONTINUE`, `BLOCKED`, `HOME`, `LEVEL`, `VICTORY`.

## Boucle, focus et accessibilité

- `requestAnimationFrame` : additionner le delta réel et conserver les fractions de milliseconde, puis appeler `advance(Math.floor(delta))`. Plafonner un delta d'interface à 100 ms pour éviter un rattrapage brutal après un blocage. Le moteur intègre en pas fixes de 20 ms.
- Mettre en pause sur `visibilitychange` vers masqué et à la perte de focus. La reprise doit rester explicite. Ne pas continuer un chrono derrière une boîte d'aide ou de confirmation.
- Flèches, WASD et ZQSD pour les directions ; P/Échap pour pause/reprise ; Entrée pour l'action principale. Ne pas intercepter les flèches d'un formulaire ou le défilement hors zone de jeu.
- Croix tactile : boutons nommés de 44 px minimum, `pointerdown`/`pointerup`/`pointercancel`, répétition éventuelle bornée. Aucun nouveau tick moteur dans les callbacks des commandes.
- Canvas accessible par un nom et une description des règles. Contrôles et messages réels dans le DOM. Restaurer le focus après les modales et après un retour au jeu.
- `prefers-reduced-motion` retire les mouvements décoratifs, secousses et transitions ; le mouvement indispensable au jeu reste actif.
- Stockage local facultatif pour les records **séparés par difficulté** et préférences. Gérer les erreurs de stockage ; aucun compte ni backend requis.

## Règles à afficher

Balade : 5 vies, 70 s, vitesse ×0,72. Classique : 3 vies, 55 s, vitesse ×1. Express : 3 vies, 45 s, vitesse ×1,25. Trois étapes : vitesse ×1, puis ×1,16, puis ×1,32 ; espacement routier réduit de 0,25 case par étape. Le temps est renouvelé après chaque refuge ou vie perdue.

Score : 10 points par nouvelle rangée atteinte sur une traversée, sans gain pour les allers-retours ; 200 + 2 × secondes entières restantes par refuge ; 500 par étape complète. Les points déjà obtenus restent après une vie perdue. Viser un refuge occupé ou un bord invalide en haut bloque le saut, sans retirer une vie ni attribuer de points.

## Distribution et validation attendues côté frontend

- Conserver `base: './'` et des liens d'assets relatifs pour l'intégration sous `/frogger/`.
- `npm run build:ui` compile seulement le frontend existant ; `npm run build` reconstruit aussi le moteur via le wrapper Maven.
- Ne pas commiter `node_modules`, `dist` ou le JS généré. Le build CI régénère le moteur depuis les sources Java.
- Avant livraison : chargement sans erreur, partie clavier et tactile, pause/focus/aide, toutes les transitions de fin, reset confirmé, affichage des vies/temps/refuges, mobile et mouvement réduit.
- La CI existante couvre Java et le module TeaVM. Ajouter la construction et la validation du frontend lors de l'intégration de l'interface terminée.
- Embarquer les textes exacts `docs/licenses/TeaVM-LICENSE.txt` et `TeaVM-NOTICE.txt` dans la distribution web, avec les notices OFL des polices retenues et la licence du code original. Les copies sources sont déjà présentes ; aucun fichier frontend n'a été modifié pour cette étape.
- À ce passage de relais, aucune validation de ces parcours web n'est revendiquée : le frontend est incomplet. Les tests JVM et la comparaison du module TeaVM sont des validations distinctes du navigateur.

## Origine

L'historique Git de novembre 2021 reste intact. L'auteur enregistré des trois anciens commits est `bjrc`. Les sources Java historiques et le JAR fourni restent consultables au commit `4679499`. La nouvelle version n'utilise ni ne distribue ce JAR sans sources/licence documentées. Voir `docs/HISTORIQUE.md` et les notices de distribution.
