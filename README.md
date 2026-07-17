# PolyCard

## Usage

The mod adds multiple cards with different rarities and effects.
Each card at a given rarity has the effect of that rarity combined with the effects of all lower rarities.
Each card has a unique method of obtaining it with a cumulative probability threshold for each rarity.
For example, a 20% Common threshold and a 7% Uncommon threshold mean a 20% chance to receive
any card, including a 7% chance for Uncommon or better.
More details on each card and how to obtain them can be queried using the /polycard info command.

A player can equip up to 5 cards at a time, and the effects of those cards will be applied to the player.
To equip a card, the player can use the /polycard equip command,
which opens a GUI where they can select which cards to equip, or by right-clicking when holding a card.

All gameplay logic is server-side.

10 cards of the same type and rarity can be combined to create a card of the same type
but one rarity higher using the /polycard combine command.

Achievements track the player's progress in obtaining cards.

## Commands

- /polycard help
    - Displays a list of available commands and their descriptions.


- /polycard info \<cardType>
    - Get information about a specific card.


- /polycard equip \[player]
    - Open the equipment manager to equip up to 5 cards.
    - If a player is specified, open the equipment manager for that player (admins only).


- /polycard combine
  - Combine the cards the player is holding into a new card of a greater rarity.


- /polycard cooldown
  - Gets the currently active cooldowns for a player


- /polycard give \<players> \<cardType> \[rarityLevel]
    - Give a card to one or more players (admins only).


- /polycard test \<cardType> \[cardsNumber]
    - Test the card rolling system (admins only).