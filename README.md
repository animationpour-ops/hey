# Divine Fishing — mod Forge 1.20.1

Un système de pêche qui fait sortir de l'eau des **items divins** (cheat), chacun avec de **vrais effets visuels custom** (pas les particules de base de Minecraft : un petit moteur de rendu additif maison dessine éclairs, anneaux, trou noir, ailes, etc.).

## Les items

| Item | Comment l'avoir | Effet |
|---|---|---|
| **Canne Divine** | craft : canne à pêche + étoile du Nether + 2 diamants (ou onglet créatif) | Incassable, appât ultra rapide (Lure +5, Chance +3), ~45 % de chance de pêcher un item divin |
| **Lame du Jugement** | pêche | Chaque coup foudroie la cible. Clic droit : 3 éclairs du ciel + chaîne d'arcs électriques |
| **Orbe du Néant** | pêche | Trou noir de 6 s (disque d'accrétion, particules aspirées) qui aspire et écrase, puis s'effondre en onde de choc |
| **Sablier de Chronos** | pêche | Arrêt du temps 8 s dans 40 blocs : mobs, joueurs et projectiles figés, cadran doré au sol, poussière figée, teinte d'écran |
| **Sceptre Météore** | pêche | 1 gros météore + 4 petits, cercle d'avertissement, traînée de feu, cratère, magma |
| **Cœur de Phénix** | pêche | Clic droit : soin total + ailes de flammes. Dans l'inventaire : annule ta mort (recharge 5 min) |
| **Plume d'Ange** | pêche | Vol 90 s avec ailes lumineuses et halo. Reclique pour te poser |

Avec une canne normale, la chance de pêcher un item divin est de 3 % (+2 % par niveau de Chance de la mer).

## Compiler avec GitHub (sans rien installer)

1. Crée un dépôt sur GitHub, puis envoie tout le contenu du dossier (y compris `.github/`).
2. Onglet **Actions** > workflow **Build mod** (il se lance tout seul à chaque push, ou bouton *Run workflow*).
3. Quand il est vert, ouvre l'exécution et télécharge **divinefishing-jar** en bas de page : le `.jar` est dedans.
4. Mets ce `.jar` dans le dossier `mods` d'un Minecraft **1.20.1 avec Forge 47.x**.

Si le build est rouge, ouvre l'étape **Compiler** : l'erreur s'y trouve (ligne `error:` avec le fichier et la ligne).
Pour une release avec le `.jar` attaché : `git tag v1.0.0 && git push --tags`.

## Compiler et tester en local

Il faut **JDK 17**.

```bash
./gradlew build        # produit build/libs/divinefishing-1.0.0.jar -> à mettre dans le dossier mods de Forge 1.20.1 (47.x)
./gradlew runClient    # lance Minecraft directement pour tester
```

Ou ouvrir le dossier dans IntelliJ IDEA (import Gradle automatique), puis lancer la configuration `runClient`.

Pour tester vite : en créatif, prends les items dans l'onglet « Pêche Divine ».

## Où modifier quoi

- Chances et poids de pêche : `loot/DivineFishingModifier.java`
- Dégâts, portées, durées : `server/Abilities.java`, `BlackHoleEffect.java`, `TimeStopEffect.java`
- Recharges : `registry/ModItems.java`
- Effets visuels : `client/Fx*.java` (un fichier par famille d'effet), primitives de dessin dans `client/VfxDraw.java`
- Textures : `assets/divinefishing/textures/` (items 16x16 et textures d'effets dans `vfx/`)
- Textes : `assets/divinefishing/lang/`

Les cratères (météore, trou noir) respectent la règle `mobGriefing`.
