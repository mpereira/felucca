/*global BattleArena, $*/
'use strict';

window.BattleArena = {
  Models: {},
  Collections: {},
  Views: {},
  Config: {
    verticalTilesCount: 16,
    horizontalTilesCount: 16,
    tileWidth: 40,
    tileHeight: 40,
    heroWidth: 40,
    heroHeight: 40,
    baseWidth: 120,
    baseHeight: 120,
    topBaseFill: 'blue',
    bottomBaseFill: 'red',
    heroAttackSpeed: 3,
    heroAttackRange: Math.sqrt(Math.pow(40, 2) + Math.pow(40, 2)), // UPDATE
    heroDamage: 1,
    minimumHeroStrength: 10,
    maximumHeroStrength: 100,
    topHeroFill: 'cyan',
    topHeroMovementSpeed: 1,
    bottomHeroFill: 'orange',
    bottomHeroMovementSpeed: 1,
    tileStroke: '#bbb',
    tileStrokeWidth: '1',
    shouldHighlightOccupiedTiles: true,
    walkableTileFill: '#83AF9B',
    nonWalkableTileFill: '#ddd',
    tileClickHighlightFill: 'green',
    tileClickHighlightDuration: 500,
    shouldHighlightPathfinding: true,
    tilePathfindingHighlightFill: '#a4deb2',
    movementHandlerDelay: 10,
    attackHandlerDelay: 100
  },
  init: function () {
    this.stage = new Kinetic.Stage({
      container: 'container',
      width: 640,
      height: 640
    });

    this.mapLayer = new Kinetic.Layer();
    this.basesLayer = new Kinetic.Layer();
    this.heroesLayer = new Kinetic.Layer();
    this.valueBarsLayer = new Kinetic.Layer();

    this.map = new BattleArena.Models.Map({
      width: this.Config.verticalTilesCount * this.Config.tileWidth,
      height: this.Config.horizontalTilesCount * this.Config.tileHeight,
      tileWidth: this.Config.tileWidth,
      tileHeight: this.Config.tileHeight
    });

    this.mapView = new BattleArena.Views.Map({
      model: this.map,
      layer: this.mapLayer
    });
    this.mapView.render();

    this.bottomBase = new BattleArena.Models.Base({
      x: this.Config.tileWidth,
      y: (this.Config.horizontalTilesCount * this.Config.tileHeight) -
           this.Config.baseHeight -
           this.Config.tileHeight,
      width: this.Config.baseWidth,
      height: this.Config.baseHeight,
      fill: this.Config.bottomBaseFill
    });

    this.bottomBaseView = new BattleArena.Views.Base({
      model: this.bottomBase,
      layer: this.basesLayer
    });
    this.bottomBaseView.render();

    this.topBase = new BattleArena.Models.Base({
      x: (this.Config.verticalTilesCount * this.Config.tileWidth) -
           this.Config.baseWidth -
           this.Config.tileWidth,
      y: this.Config.tileWidth,
      width: this.Config.baseWidth,
      height: this.Config.baseHeight,
      fill: this.Config.topBaseFill
    });

    this.topBaseView = new BattleArena.Views.Base({
      model: this.topBase,
      layer: this.basesLayer
    });
    this.topBaseView.render();

    this.bottomHero = new BattleArena.Models.Hero({
      movementSpeed: this.Config.bottomHeroMovementSpeed,
      x: this.bottomBase.get('x') + this.Config.baseWidth + this.Config.tileWidth,
      y: this.bottomBase.get('y') - 2 * this.Config.tileWidth,
      width: this.Config.heroWidth,
      height: this.Config.heroHeight,
      fill: this.Config.bottomHeroFill
    });

    this.bottomHeroView = new BattleArena.Views.Hero({
      model: this.bottomHero,
      layer: this.heroesLayer,
      hitPointsValueBarLayer: this.valueBarsLayer
    });
    this.bottomHeroView.render();

    this.topHero = new BattleArena.Models.Hero({
      movementSpeed: this.Config.topHeroMovementSpeed,
      x: this.topBase.get('x') - 2 * this.Config.tileWidth,
      y: this.topBase.get('y') + this.Config.baseHeight + this.Config.tileHeight,
      width: this.Config.heroWidth,
      height: this.Config.heroHeight,
      fill: this.Config.topHeroFill
    });

    this.topHeroView = new BattleArena.Views.Hero({
      model: this.topHero,
      layer: this.heroesLayer,
      hitPointsValueBarLayer: this.valueBarsLayer
    });
    this.topHeroView.render()

    this.mapLayer.add(this.mapView.group);
    this.basesLayer.add(this.bottomBaseView.group);
    this.basesLayer.add(this.topBaseView.group);
    this.heroesLayer.add(this.bottomHeroView.group);
    this.heroesLayer.add(this.topHeroView.group);

    this.stage.add(this.mapLayer);
    this.stage.add(this.basesLayer);
    this.stage.add(this.heroesLayer);
    this.stage.add(this.valueBarsLayer);

    this.mapLayer.setZIndex(0);
    this.basesLayer.setZIndex(1);
    this.heroesLayer.setZIndex(2);
    this.valueBarsLayer.setZIndex(3);

    this.objects = new BattleArena.Collections.Objects();

    this.objectSpace = new BattleArena.Models.ObjectSpace({
      objects: this.objects,
      tiles: this.map.get('tiles')
    });

    this.hero = this.bottomHero;
    this.heroView = this.bottomHeroView;
    this.pathfinder = new BattleArena.Models.Pathfinder({
      hero: this.hero,
      tiles: this.map.get('tiles'),
      tilesView: this.mapView.tilesView
    });

    var objects = this.objects;
    _([
       this.bottomBase,
       this.topBase,
       this.bottomHero,
       this.topHero
    ]).each(function(object) {
      objects.add(object);
    });

    var stats = new Stats();
    stats.setMode(0);

    stats.domElement.style.position = 'fixed';
    stats.domElement.style.left = '0px';
    stats.domElement.style.top = '0px';

    document.body.appendChild(stats.domElement);

    setInterval(function () { stats.update(); }, 1000 / 60);
  }
};

BattleArena.Utils = {
  getValue: function(valueOrFunction) {
    return(_(valueOrFunction).isFunction() ? valueOrFunction() : valueOrFunction);
  },

  mixin: function(targetInstance, source, attributesOrOptions) {
    attributesOrOptions || (attributesOrOptions = {});

    _(_(_(source.prototype).keys()).difference([
      'constructor', 'initialize'
    ])).each(function(methodName, index, methodNames) {
      targetInstance.constructor.prototype[methodName] = source.prototype[methodName];
    });

    if (source.prototype.set) {
      source.prototype.set.call(targetInstance, _(attributesOrOptions).extend({
        mixee: targetInstance
      }));
    } else {
     targetInstance.options = _(targetInstance.options).extend(attributesOrOptions);
    }

    source.prototype.initialize.call(targetInstance);
  },

  pixelDistance: function(ax, ay, bx, by) {
    return(Math.sqrt(Math.pow(ax - bx, 2) + Math.pow(ay - by, 2)));
  }
}

BattleArena.Models.Tile = Backbone.Model.extend({
  initialize: function() {
    this.set('objects', new BattleArena.Collections.Objects());
  },

  isWalkable: function() {
    return(this.get('objects').size() === 0);
  }
});

BattleArena.Collections.Tiles = Backbone.Collection.extend({
});

BattleArena.Views.Tile = Backbone.View.extend({
  initialize: function() {
    this.layer = this.options.layer;
    this.group = new Kinetic.Group();
    this.square = new Kinetic.Rect();
    this.group.add(this.square);

    this.square.setAttrs({
      x: this.model.get('x'),
      y: this.model.get('y'),
      width: this.model.get('width'),
      height: this.model.get('height'),
      stroke: BattleArena.Config.tileStroke,
      strokeWidth: BattleArena.Config.tileStrokeWidth,
      fill: BattleArena.Config.walkableTileFill
    });

    this.model.get('objects').on(
      'reset add remove', this.onModelObjectsChange, this
    );
  },

  onModelObjectsChange: function(tile, options) {
    var fill;

    if (BattleArena.Config.shouldHighlightOccupiedTiles && !this.model.isWalkable()) {
      fill = BattleArena.Config.nonWalkableTileFill
    } else {
      fill = BattleArena.Config.walkableTileFill
    }

    this.square.setAttr('fill', fill);
    this.render();
  },

  render: function() {
    this.layer.draw();
    return(this);
  }
});


BattleArena.Views.Tiles = Backbone.Marionette.CollectionView.extend({
  itemView: BattleArena.Views.Tile,

  initialize: function() {
    this.layer = this.options.layer;
    this.group = new Kinetic.Group();
  },

  buildItemView: function(item, ItemViewType, itemViewOptions) {
    return(new ItemViewType(_({
      model: item, layer: this.layer
    }).extend(itemViewOptions)));
  },

  onRender: function() {
    var self = this;
    this.children.each(function(view) { self.group.add(view.group); });
  }
});

BattleArena.Models.Map = Backbone.Model.extend({
  initialize: function() {
    if (!this.has('tiles')) {
      this.set('tiles', new BattleArena.Collections.Tiles());

      for (var x = 0; x < this.get('width'); x += this.get('tileWidth')) {
        for (var y = 0; y < this.get('height'); y += this.get('tileHeight')) {
          this.get('tiles').add(new BattleArena.Models.Tile({
            x: x,
            y: y,
            width: this.get('tileWidth'),
            height: this.get('tileHeight')
          }));
        }
      }
    }
  }
});

BattleArena.Views.Map = Backbone.View.extend({
  initialize: function() {
    this.layer = this.options.layer;
    this.group = new Kinetic.Group();

    this.tilesView = new BattleArena.Views.Tiles({
      collection: this.model.get('tiles'),
      layer: this.layer
    });

    this.group.add(this.tilesView.group);
  },

  render: function() {
    this.tilesView.render();
    return(this);
  }
});

BattleArena.Models.Base = Backbone.Model.extend({
});

BattleArena.Views.Base = Backbone.View.extend({
  initialize: function() {
    this.group = new Kinetic.Group();
    this.layer = this.options.layer;

    var square = new Kinetic.Rect({
      x: this.model.get('x'),
      y: this.model.get('y'),
      width: this.model.get('width'),
      height: this.model.get('height'),
      stroke: 'green',
      fill: this.model.get('fill'),
      strokeWidth: 4
    });

    this.group.add(square);
  },

  render: function() {
    this.layer.draw();
    return(this);
  }
});

BattleArena.Models.Hero = Backbone.Model.extend({
  initialize: function() {
    BattleArena.Utils.mixin(this, BattleArena.Models.Attacker);
    BattleArena.Utils.mixin(this, BattleArena.Models.Attackable);
    BattleArena.Utils.mixin(this, BattleArena.Models.Distanceable);

    this.movable = new BattleArena.Models.Movable(this);
    this.pathfindable = new BattleArena.Models.Pathfindable(this);

    this.strength = new BattleArena.Models.CappedAttribute({
      mixee: this,
      name: 'strength',
      minimum: BattleArena.Config.minimumHeroStrength,
      maximum: BattleArena.Config.maximumHeroStrength
    });

    var hero = this;

    this.hitPoints = new BattleArena.Models.CappedAttribute({
      mixee: this,
      name: 'hitPoints',
      minimum: 0,
      value: this.get('strength'),
      maximum: function() {
        return(hero.get('strength'));
      }
    });

    this.hitPointsBar = new BattleArena.Models.ValueBar({
      x: this.get('x') + (this.get('width') - this.get('width') * 0.75) / 2,
      y: this.get('y') - 6 * 2,
      width: this.get('width') * 0.75,
      height: 6,
      fill: 'red',
      stroke: 'black',
      strokeWidth: 1,
      minimumValue: this.hitPoints.get('minimum'),
      maximumValue: this.hitPoints.get('maximum'),
      value: function() {
        return(hero.get('hitPoints'));
      },
      onInitialize: function(valueBar) {
        hero.on('change:hitPoints', function(hero, value, options) {
          valueBar.set('hitPoints', value);
        });

        hero.on('change:x', function(hero, value, options) {
          valueBar.set(
            'x',
            hero.get('x') + (hero.get('width') - hero.get('width') * 0.75) / 2
          );
        });

        hero.on('change:y', function(hero, value, options) {
          valueBar.set('y', hero.get('y') - 6 * 2);
        });
      }
    });
  },

  isAlive: function() {
    return(this.get('hitPoints') > 0);
  },

  isDead: function() {
    return(!this.isAlive());
  }
});

BattleArena.Views.Hero = Backbone.View.extend({
  initialize: function() {
    this.layer = this.options.layer;

    this.group = new Kinetic.Group({
      x: this.model.get('x'),
      y: this.model.get('y')
    });

    this.square = new Kinetic.Rect({
      width: this.model.get('width'),
      height: this.model.get('height'),
      stroke: 'yellow',
      fill: this.model.get('fill'),
      strokeWidth: 2
    });

    this.layer.add(this.group);

    BattleArena.Utils.mixin(this, BattleArena.Views.Attackable, {
      model: this.model,
      modelView: this
    });

    this.hitPointsBarView = new BattleArena.Views.ValueBar({
      model: this.model.hitPointsBar,
      layer: this.options.hitPointsValueBarLayer
    });

    this.group.add(this.square);

    this.model.on('change:x change:y', this.onChangeXOrChangeY, this);
  },

  onChangeXOrChangeY: function(hero, options) {
    this.group.setAttrs({ x: this.model.get('x'), y: this.model.get('y') });
    this.render();
  },

  render: function() {
    this.layer.draw();
    return(this);
  }
});

BattleArena.Collections.Objects = Backbone.Collection.extend({
});

BattleArena.Models.ObjectSpace = Backbone.Model.extend({
  initialize: function() {
    this.get('objects').on('add', this.onObjectAdd, this);
    this.get('objects').on('remove', this.onObjectRemove, this);

    var objectSpace = this;

    this.get('objects').each(function(object) {
      object.on('change:x change:y', objectSpace.onObjectMovement, objectSpace);
    });
  },

  tilesOccupiedByObjectWithAttributes: function(x, y, width, height) {
    var minimumX = x - (x % BattleArena.Config.tileWidth);
    var minimumY = y - (y % BattleArena.Config.tileHeight);
    var maximumX = x +
                     width -
                     (((x + width) % BattleArena.Config.tileWidth) ||
                        BattleArena.Config.tileWidth);
    var maximumY = y +
                     height -
                     (((y + height) % BattleArena.Config.tileHeight) ||
                        BattleArena.Config.tileHeight);
    var tiles = [];

    for (var x = minimumX; x <= maximumX; x += BattleArena.Config.tileWidth) {
      for (var y = minimumY; y <= maximumY; y += BattleArena.Config.tileHeight) {
        tiles = tiles.concat(this.get('tiles').where({ x: x, y: y }));
      }
    }

    return(tiles);
  },

  tilesOccupiedBy: function(object) {
    return(this.tilesOccupiedByObjectWithAttributes(
      object.get('x'),
      object.get('y'),
      object.get('width'),
      object.get('height')
    ));
  },

  onObjectAdd: function(object, objects, options) {
    object.on('change:x change:y', this.onObjectMovement, this);

    var objectSpace = this;
    _(this.tilesOccupiedBy(object)).each(function(tile) {
      tile.get('objects').add(object);
    });
  },

  onObjectRemove: function(object, objects, options) {
    object.off('change:x change:y', this.onObjectMovement, this);

    var objectSpace = this;
    _(this.tilesOccupiedBy(object)).each(function(tile) {
      tile.get('objects').remove(object);
    });
  },

  onObjectMovement: function(object) {
    var previouslyOccupiedTiles = this.tilesOccupiedByObjectWithAttributes(
      object.previous('x'),
      object.previous('y'),
      object.previous('width'),
      object.previous('height')
    );

    var currentlyOccupiedTiles = this.tilesOccupiedByObjectWithAttributes(
      object.get('x'),
      object.get('y'),
      object.get('width'),
      object.get('height')
    );

    var objectSpace = this;
    _(_(previouslyOccupiedTiles).difference(currentlyOccupiedTiles)).each(function(tile) {
      tile.get('objects').remove(object);
    });

    _(_(currentlyOccupiedTiles).difference(previouslyOccupiedTiles)).each(function(tile) {
      tile.get('objects').add(object);
    });
  }
});

BattleArena.Models.Pathfinder = Backbone.Model.extend({
  initialize: function() {
    this.finder = new PF.AStarFinder({
      allowDiagonal: true,
      dontCrossCorners: true
    });

    if (BattleArena.Config.shouldHighlightPathfinding) {
      this.get('hero').on('change:path', this.highlightPath, this);
    }

    this.set('grid', new PF.Grid(
      BattleArena.Config.verticalTilesCount,
      BattleArena.Config.horizontalTilesCount
    ));

    var pathfinder = this;

    this.get('tilesView').children.each(function(tileView) {
      tileView.square.on('mousedown touchstart', function(event) {
        if (event.which !== 3) {
          return;
        }

        pathfinder.pathfindToTile(tileView.model);
        pathfinder.highlightTile(tileView.model);
      }, pathfinder);
    });

    this.get('tiles').each(function(tile) {
      tile.get('objects').on('add', function(tileObject, tileObjects, options) {
        pathfinder.onTileObjectAdd(tile, tileObject, tileObjects, options);
      }, pathfinder);

      tile.get('objects').on('remove', function(tileObject, tileObjects, options) {
        pathfinder.onTileObjectRemove(tile, tileObject, tileObjects, options);
      }, pathfinder);
    });
  },

  highlightTile: function(tile) {
    var tileView = this.get('tilesView').children.find(function(tileView) {
      return(tileView.model === tile);
    });

    tileView.square.setAttr(
      'fill', BattleArena.Config.tileClickHighlightFill
    );

    _(function() {
      tileView.square.setAttr('fill', BattleArena.Config.walkableTileFill);
      tileView.layer.draw();
    }).delay(BattleArena.Config.tileClickHighlightDuration);
  },

  highlightPath: function(hero, path, options) {
    if (_(this.get('hero').get('path')).isEmpty()) {
      return;
    }

    var pathfinder = this;

    var previousPathTiles =
      _(_(this.get('hero').previous('path')).tail()).map(function(tileCoordinates) {
        return(_(tileCoordinates).map(function(tileCoordinate) {
          return(tileCoordinate * BattleArena.Config.tileWidth);
        }))
      }).reduce(function(memo, coordinates) {
        return(memo.concat(pathfinder.get('tiles').where({
          x: coordinates[0], y: coordinates[1]
        })));
      }, []);

    var currentPathTiles =
      _(_(this.get('hero').get('path')).tail()).map(function(tileCoordinates) {
        return(_(tileCoordinates).map(function(tileCoordinate) {
          return(tileCoordinate * BattleArena.Config.tileWidth);
        }))
      }).reduce(function(memo, coordinates) {
        return(memo.concat(pathfinder.get('tiles').where({
          x: coordinates[0], y: coordinates[1]
        })));
      }, []);

    _(_(previousPathTiles).difference(currentPathTiles)).each(function(tile) {
      var tileView = pathfinder.get('tilesView').children.findByModel(tile);
      tileView.square.setAttr('fill', BattleArena.Config.walkableTileFill);
    });

    _(_(currentPathTiles).difference(previousPathTiles)).each(function(tile) {
      var tileView = pathfinder.get('tilesView').children.findByModel(tile);
      tileView.square.setAttr('fill', BattleArena.Config.tilePathfindingHighlightFill);
    });
  },

  pathfindToTile: function(tile) {
    var path = _(this.finder.findPath(
      Math.floor(this.get('hero').get('x') / BattleArena.Config.tileWidth),
      Math.floor(this.get('hero').get('y') / BattleArena.Config.tileHeight),
      Math.floor(tile.get('x') / BattleArena.Config.tileWidth),
      Math.floor(tile.get('y') / BattleArena.Config.tileHeight),
      this.get('grid').clone()
    )).tail();

    if (!path || !path[0] || !path[0][0] || !path[0][1]) {
      return;
    }

    this.get('hero').set('path', path);
  },

  onTileObjectAdd: function(tile, tileObject, tileObjects, options) {
    this.get('grid').setWalkableAt(
      tile.get('x') / BattleArena.Config.tileWidth,
      tile.get('y') / BattleArena.Config.tileHeight,
      tile.isWalkable()
    );
  },

  onTileObjectRemove: function(tile, tileObject, tileObjects, options) {
    this.get('grid').setWalkableAt(
      tile.get('x') / BattleArena.Config.tileWidth,
      tile.get('y') / BattleArena.Config.tileHeight,
      tile.isWalkable()
    );
  }
});

BattleArena.Models.Movable = Backbone.Model.extend({
  initialize: function(movable) {
    this.movable = movable

    this.movementHandlerIntervalId = setInterval(
      _(this.movementHandler).bind(this),
      BattleArena.Config.movementHandlerDelay
    );
  },

  movementHandler: function() {
    var x = this.movable.get('x');
    var y = this.movable.get('y');

    if (this.movable.get('x') > this.movable.get('destinationX')) {
      x -= this.movable.get('movementSpeed');
    } else if (this.movable.get('x') < this.movable.get('destinationX')) {
      x += this.movable.get('movementSpeed');
    }

    if (this.movable.get('y') > this.movable.get('destinationY')) {
      y -= this.movable.get('movementSpeed');
    } else if (this.movable.get('y') < this.movable.get('destinationY')) {
      y += this.movable.get('movementSpeed');
    }

    this.movable.set({ x: x, y: y });
  }
});

BattleArena.Models.Pathfindable = Backbone.Model.extend({
  initialize: function(pathfindable) {
    this.set('pathfindable', pathfindable);

    this.get('pathfindable').set('path', []);

    this.get('pathfindable').on('change:path', this.setDestinationWithThePathsHead, this);
    this.get('pathfindable').on(
      'change:x change:y change:destinationX change:destinationY',
      this.setPathToItsTailIfOnIntermediateDestination,
      this
    );
  },

  setDestinationWithThePathsHead: function(creature, value, options) {
    if (_(this.get('pathfindable').get('path')).isEmpty()) {
      return;
    }

    var pathHead = _(this.get('pathfindable').get('path')).head();

    this.get('pathfindable').set({
      destinationX: pathHead[0] * BattleArena.Config.tileWidth,
      destinationY: pathHead[1] * BattleArena.Config.tileHeight
    });
  },

  setPathToItsTailIfOnIntermediateDestination: function(creature, value, options) {
    if (_(this.get('pathfindable').get('path')).isEmpty()) {
      return;
    }

    if (this.get('pathfindable').get('x') ===
          this.get('pathfindable').get('destinationX') &&
          this.get('pathfindable').get('y') ===
            this.get('pathfindable').get('destinationY')) {
      this.get('pathfindable').set(
        'path',
        _(this.get('pathfindable').get('path')).tail()
      );
    }
  }
});

BattleArena.Models.CappedAttribute = Backbone.Model.extend({
  initialize: function() {
    var cappedAttribute = this;

    _(['minimum', 'maximum']).each(function(bound) {
      if (!_(cappedAttribute.get(bound)).isNumber() &&
            !_(cappedAttribute.get(bound)).isFunction()) {
        throw(new Error('"' + bound + '" attribute must be an integer or a function'));
      }
    });

    this.get('mixee').set(
      this.get('name'),
      this.has('value') ? this.get('value') :
                          BattleArena.Utils.getValue(this.get('minimum'))
    );
  },

  normalizedSet: function(value) {
    if (value < BattleArena.Utils.getValue(this.get('minimum'))) {
      value = BattleArena.Utils.getValue(this.get('minimum'));
    } else if (value > BattleArena.Utils.getValue(this.get('maximum'))) {
      value = BattleArena.Utils.getValue(this.get('maximum'));
    } else {
      value = Number(value.toFixed(1));
    }

    this.get('mixee').set(this.get('name'), value);

    return(this.get('mixee'));
  },

  decrease: function(quantity) {
    return(this.normalizedSet(this.get('mixee').get(this.get('name')) - quantity));
  },

  increase: function(quantity) {
    return(this.normalizedSet(this.get('mixee').get(this.get('name')) + quantity));
  }
});

BattleArena.Models.ValueBar = Backbone.Model.extend({
  initialize: function() {
    this.get('onInitialize')(this);
  }
});

BattleArena.Views.ValueBar = Backbone.View.extend({
  initialize: function() {
    this.layer = this.options.layer;

    this.group = new Kinetic.Group({
      x: this.model.get('x'),
      y: this.model.get('y')
    });

    this.rectangle = new Kinetic.Rect({
      height: this.model.get('height'),
      fill: this.model.get('fill'),
      stroke: this.model.get('stroke'),
      strokeWidth: this.model.get('strokeWidth')
    });

    this.updateWidth();

    this.group.add(this.rectangle);
    this.layer.add(this.group);

    this.model.on('change', this.updateWidth, this);
    this.model.on('change:x change:y', this.updatePosition, this);
  },

  updateWidth: function() {
    this.rectangle.setAttrs({
      width: this.model.get('width') *
               this.model.get('value')() /
                 BattleArena.Utils.getValue(this.model.get('maximumValue'))
    });

    this.render();
  },

  updatePosition: function() {
    this.group.setAttrs({
      x: this.model.get('x'),
      y: this.model.get('y')
    });

    this.render();
  },

  render: function() {
    this.layer.draw();
    return(this);
  }
});

BattleArena.Models.Attacker = Backbone.Model.extend({
  initialize: function() {
    this.handlerId = setInterval(
      _(this.handler).bind(this), BattleArena.Config.attackHandlerDelay
    );

    this.get('mixee').set('attackee', null);
    this.get('mixee').set('lastHitAttemptedAt', Number.NEGATIVE_INFINITY);

    this.get('mixee').on('change:attackee', this.onChangeMixeeAttackee, this);
  },

  onChangeMixeeAttackee: function(mixee, attackee, options) {
    if (this.get('mixee').previous('attackee')) {
      this.get('mixee').previous('attackee').off(
        'change:hitPoints', this.onChangeMixeeAttackeeHitPoints, this
      );
    }

    if (this.get('mixee').has('attackee')) {
      this.get('mixee').get('attackee').on(
        'change:hitPoints', this.onChangeMixeeAttackeeHitPoints, this
      );
    }
  },

  onChangeMixeeAttackeeHitPoints: function(attackee, hitPoints, options) {
    if (this.get('mixee').has('attackee') && this.get('mixee').get('attackee').isDead()) {
      this.get('mixee').stopAttacking(this.get('mixee').get('attackee'));
    }
  },

  attackRange: function() {
    return(BattleArena.Config.heroAttackRange);
  },

  attackSpeed: function() {
    return(BattleArena.Config.heroAttackSpeed * 1000);
  },

  isWithinAttackRange: function(creature) {
    return(this.get('mixee').pixelDistance(creature) <= this.get('mixee').attackRange())
  },

  hit: function(creature) {
    creature.receiveHit(this.get('mixee'), BattleArena.Config.heroDamage);
  },

  attemptHit: function(creature) {
    this.get('mixee').set('lastHitAttemptedAt', Date.now());
    this.get('mixee').hit(creature);
  },

  attack: function(attackee) {
    return(this.get('mixee').set('attackee', attackee));
  },

  stopAttacking: function(creature) {
    return(this.get('mixee').set('attackee', null));
  },

  handler: function() {
    if (!this.get('mixee').has('attackee')) {
      return;
    }

    if (this.get('mixee').get('attackee').isDead()) {
      return;
    }

    if (!this.get('mixee').isWithinAttackRange(this.get('mixee').get('attackee'))) {
      return;
    }

    if ((Date.now() - this.get('mixee').get('lastHitAttemptedAt')) >=
          this.get('mixee').attackSpeed()) {
      this.get('mixee').attemptHit(this.get('mixee').get('attackee'));
    }
  }
});

BattleArena.Models.Attackable = Backbone.Model.extend({
  initialize: function() {
    this.get('mixee').set('attackers', new Backbone.Collection());
  },

  receiveHit: function(attacker, damage) {
    this.get('mixee').hitPoints.decrease(damage);
  }
});

BattleArena.Views.Attackable = Backbone.View.extend({
  initialize: function() {
    this.modelView = this.options.modelView;

    this.modelView.group.on('dblclick dbltap', _(this.onDoubleClick).bind(this));
  },

  onDoubleClick: function() {
    if (this.model.get('mixee').isDead()) {
      return;
    }

    if (this.model.get('mixee') === BattleArena.hero) {
      return;
    }

    BattleArena.hero.attack(this.model.get('mixee'));
  }
});

BattleArena.Models.Distanceable = Backbone.Model.extend({
  pixelDistance: function(thing) {
    return(BattleArena.Utils.pixelDistance(
      this.get('mixee').get('x'),
      this.get('mixee').get('y'),
      thing.get('x'),
      thing.get('y')
    ));
  }
});

$(document).ready(function () {
  $('#container').on('contextmenu', function(event) { return(false); });
  BattleArena.init();
});
