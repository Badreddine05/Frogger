# Origine et évolution

Ce dépôt conserve ses trois commits historiques du 24 novembre 2021, sous l'auteur Git `bjrc`. Le dernier état initial est `4679499`. Ces informations sont celles du dépôt ; elles ne permettent pas d'attribuer personnellement chaque ligne à Badreddine Channoufi.

Le projet initial est une application Java Swing. Il contient la grenouille, l'orchestration et le rendu, ainsi qu'un JAR fourni nommé `givenEnvironment.jar`, avec les classes `GivenEnvironment`, `Lane` et `Car`. Les sources et la licence de cette bibliothèque ne sont pas documentées dans l'état initial.

La version 2 remplace cette dépendance par un moteur original, avec des voies, des véhicules et des troncs explicitement représentés dans le code. Le JAR fourni, les classes déjà compilées et les fichiers propres à IntelliJ ne font plus partie de la nouvelle distribution. L'historique Git n'a pas été réécrit : les versions antérieures restent consultables avec `git show 4679499:src/gameCommons/Game.java`, par exemple.

Défauts observés dans l'état initial : les commandes de déplacement appelaient aussi la mise à jour du trafic, la direction de la grenouille ne changeait pas, et les résultats de victoire/défaite étaient affichés dans la console sans terminer la partie. La version 2 sépare les entrées, le temps simulé et le rendu, puis ajoute les états de jeu, les refuges, le courant et la progression.

Les nouveaux commits utilisent leur date réelle de réalisation. Aucun commit historique, auteur ou calendrier n'a été inventé ou antidaté. La refonte est préparée avec l'assistance de Codex ; les contributions visuelles ultérieures de l'équipe interface doivent être décrites selon les changements réellement intégrés. La présence d'une fonctionnalité dans le dépôt ne constitue pas une preuve de maîtrise personnelle sans examen ou appropriation par son propriétaire.

La licence MIT ajoutée couvre le nouveau code original de la version 2. Elle ne prétend pas modifier les droits des anciens fichiers, du JAR fourni ou d'un jeu commercial. Aucun sprite, musique ou logo commercial n'est réutilisé.
