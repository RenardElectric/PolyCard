# PolyCard Domain Glossary

## Card

A collectible item defined by one Card Type and one Rarity supported by that type. A Card can be held as an item or included in a player's Equipment.

## Card Type

A family of Cards with one stable identifier, Acquisition description, card group, supported Rarities, and set of gameplay effects. Examples include Cow, Creeper, and Totem.

## Rarity

An ordered tier from Common through Legendary. A Card at a given Rarity includes the effects introduced by all lower supported tiers. A Card Type's probability for a Rarity is cumulative: it means that rarity or better.

## Equipment

The validated set of Cards currently active for a player. A player can equip at most five Cards, no Card Type more than once, and no more than one Card from the same Mutex Group.

## Mutex Group

A named family of Card Types that cannot be equipped together. Replacing one member with another is a single Equipment change.

## Acquisition

The gameplay event or loot source that can award a Card. Acquisition first identifies a Card Type, then rolls that type's cumulative Rarity distribution.

## Effect

A gameplay behavior unlocked by equipping a Card at its required Rarity. Higher Rarities retain effects from lower supported tiers.
