- Move KineticJS shape definitions out of views.
    BattleArena.Shapes = {
    };

    BattleArena.Shapes[BattleArena.Views.Tile] = [{
      square: new Kinetic.Rect({ ... })
    }];

    // May be confusing since the view possibly has to alter shapes?

- Move KineticJS layer definitions out of views.
    BattleArena.Layers = {
    };

    BattleArena.Layers[BattleArena.Views.Tile] = BattleArena.mapLayer;

- Use computed properties.
  - https://github.com/alexanderbeletsky/backbone-computedfields
  - https://github.com/derickbailey/backbone.compute

- Break down map layer into multiple smaller layers.

- On ObjectSpace make onObjectMovement() only be called on tile change.
