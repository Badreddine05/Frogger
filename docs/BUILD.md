# Construire et vérifier Frogger

## Prérequis

Le moteur et la version bureau demandent un JDK 17 ou ultérieur. La version web demande aussi Node.js 22.12 ou ultérieur. Maven Wrapper est fourni, avec Apache Maven 3.10.0 et une empreinte SHA-256 de distribution ; aucun Maven installé globalement n'est nécessaire. La première construction télécharge les dépendances déclarées.

## Lancement bureau sans Maven

Depuis la racine du dépôt, sous PowerShell :

```powershell
./scripts/run.ps1
```

Sous Linux ou macOS :

```sh
sh scripts/run.sh
```

Ces scripts compilent uniquement le noyau et Swing avec les outils du JDK. Ils créent `target/frogger-desktop.jar`, puis lancent le menu. Le fichier produit n'a pas besoin d'une bibliothèque externe au JDK pour jouer. `JAVA_HOME` est utilisé s'il est défini ; sinon les outils Java sont recherchés dans le PATH.

Pour construire sans ouvrir la fenêtre :

```powershell
./scripts/run.ps1 -BuildOnly
java -jar target/frogger-desktop.jar
```

L'équivalent Unix est `sh scripts/run.sh --build-only`.

## Moteur, tests et module web

Sous PowerShell :

```powershell
./mvnw.cmd -Pweb verify
node --test web/tests/engine-parity.test.mjs
```

Sous Linux ou macOS :

```sh
sh mvnw -Pweb verify
node --test web/tests/engine-parity.test.mjs
```

La phase Java exécute les tests JUnit, crée `target/frogger-2.0.0.jar` et produit `web/src/generated/engine.js`. Les tests Node comparent ce module avec les classes JVM de la même construction. Il faut reconstruire Java avant le test Node si les règles changent.

Le JAR Maven contient le bridge mais la version bureau ne le charge pas : ses dépendances TeaVM servent à la compilation web. Le profil `web` ne tente pas de compiler Swing vers le navigateur.

## Frontend web

Le frontend est repris par l'équipe interface dans un espace de travail séparé, avant intégration dans ce dépôt. Le fichier `FRONTEND_HANDOFF.md` décrit l'état du travail transmis et l'API. Au passage de relais, l'interface était partielle et son point d'entrée `web/src/main.js` n'était pas encore écrit. Cette note ne vaut pas vérification de l'état actuel après reprise.

Quand son point d'entrée est présent :

```sh
npm --prefix web ci
npm --prefix web run dev
npm --prefix web run build
```

`build` reconstruit le moteur puis le frontend. `build:ui` ne reconstruit que le frontend, pour les itérations visuelles sur un moteur inchangé. Le résultat statique attendu est `web/dist/`. Vite utilise une base relative pour permettre l'hébergement sous un sous-chemin.

## Vérification native explicite

Le test d'intégration demande une session graphique. Après la construction Maven, sous Windows :

```powershell
java -cp 'target/classes;target/test-classes' frogger.desktop.DesktopSmoke
```

Sous Linux/macOS, remplacer le séparateur de classpath par `:`. Un environnement Linux sans affichage peut utiliser `xvfb-run -a` devant cette commande.

Ce test crée une vraie fenêtre Swing sur l'EDT, puis vérifie le menu, le bouton de départ, la commande gauche, la pause, le temps figé, la reprise et la fermeture. Il accepte un dossier existant en argument pour sauvegarder deux rendus. Les captures et rapports de travail ne sont pas committés dans le dépôt.

Un rendu du seul terrain peut aussi être exporté sans écran :

```powershell
./scripts/run.ps1 -Render 'C:/temp/frogger.png'
```

## CI et distribution

Le workflow CI construit le moteur Java et TeaVM, exécute les tests de règles et de parité, puis produit un rendu natif sans écran. Il ne compile pas encore le frontend en cours de reprise et ne déploie rien. Une étape de construction et de validation du frontend doit être ajoutée lors de son intégration. Les artefacts générés, caches, dépendances npm et fichiers d'IDE sont ignorés par Git.

Le code original de la version 2 est sous MIT. Les notices exactes de TeaVM 0.16.0 et de Maven Wrapper 3.3.4 sont conservées dans `docs/licenses/`, avec les licences OFL des deux polices présentes au passage de relais. Le frontend doit embarquer la licence et la notice TeaVM ainsi que les notices des assets qu'il retient. Le noyau Java natif n'utilise pas les polices web.

Les scripts du wrapper sont générés par Apache Maven Wrapper et conservent leurs en-têtes. Aucun JAR de l'ancien environnement fourni n'est utilisé dans la nouvelle construction. Les auteurs et versions initiales restent dans l'historique Git ; voir `docs/HISTORIQUE.md`.
