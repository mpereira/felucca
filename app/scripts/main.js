/*global BattleArena, $*/
'use strict';

window.BattleArena = {
  Models: {},
  Collections: {},
  Views: {},
  Config: {
    verticalTilesCount: 32,
    horizontalTilesCount: 32,
    tileWidth: 20,
    tileHeight: 20,
    heroWidth: 40,
    heroHeight: 40,
    baseWidth: 60,
    baseHeight: 60,
    topBaseFill: 'blue',
    bottomBaseFill: 'red',
    topHeroFill: 'cyan',
    bottomHeroFill: 'orange'
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

    var bottomBase = new BattleArena.Models.Base({
      x: this.Config.tileWidth,
      y: (this.Config.horizontalTilesCount * this.Config.tileHeight) -
           this.Config.baseHeight -
           this.Config.tileHeight,
      width: this.Config.baseWidth,
      height: this.Config.baseHeight,
      fill: this.Config.bottomBaseFill
    });

    var bottomBaseView = new BattleArena.Views.Base({ model: bottomBase });
    bottomBaseView.render()

    var topBase = new BattleArena.Models.Base({
      x: (this.Config.verticalTilesCount * this.Config.tileWidth) -
           this.Config.baseWidth -
           this.Config.tileWidth,
      y: this.Config.tileWidth,
      width: this.Config.baseWidth,
      height: this.Config.baseHeight,
      fill: this.Config.topBaseFill
    });

    var topBaseView = new BattleArena.Views.Base({ model: topBase });
    topBaseView.render()

    var bottomHero = new BattleArena.Models.Hero({
      x: bottomBase.get('x') + this.Config.baseWidth + this.Config.tileWidth,
      y: bottomBase.get('y') - this.Config.baseHeight,
      width: this.Config.heroWidth,
      height: this.Config.heroHeight,
      fill: this.Config.bottomHeroFill
    });

    var bottomHeroView = new BattleArena.Views.Hero({ model: bottomHero });
    bottomHeroView.render()

    var topHero = new BattleArena.Models.Hero({
      x: topBase.get('x') - this.Config.baseWidth,
      y: topBase.get('y') + this.Config.baseHeight + this.Config.tileHeight,
      width: this.Config.heroWidth,
      height: this.Config.heroHeight,
      fill: this.Config.topHeroFill
    });

    var topHeroView = new BattleArena.Views.Hero({ model: topHero });
    topHeroView.render()

    this.mapLayer.add(this.mapView.group);
    this.basesLayer.add(bottomBaseView.group);
    this.basesLayer.add(topBaseView.group);
    this.heroesLayer.add(bottomHeroView.group);
    this.heroesLayer.add(topHeroView.group);

    this.stage.add(this.mapLayer);
    this.stage.add(this.basesLayer);
    this.stage.add(this.heroesLayer);

    this.objects = new BattleArena.Collections.Objects();

    this.objectSpace = new BattleArena.Models.ObjectSpace({
      objects: this.objects,
      tiles: this.map.get('tiles')
    });

    var objects = this.objects;
    _([
       bottomBase,
       topBase,
       bottomHero,
       topHero
    ]).each(function(object) {
      objects.add(object);
    });
  }
};

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

    this.model.on('change', this.render, this);
    this.model.get('objects').on('reset add remove', this.render, this);
  },

  render: function() {
    this.square.setAttrs({
      x: this.model.get('x'),
      y: this.model.get('y'),
      width: this.model.get('width'),
      height: this.model.get('height'),
      stroke: '#ccc',
      strokeWidth: 2
    });

    if (this.model.isWalkable()) {
      this.square.setAttr('fill', 'white');
    } else {
      this.square.setAttr('fill', 'black');
    }

    this.layer.draw();
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

    this.children.each(function(view) {
      self.group.add(view.group);
    });
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
    this.layer.add(this.group);
  },

  render: function() {
    this.tilesView.render();
  }
});

BattleArena.Models.Base = Backbone.Model.extend({
});

BattleArena.Views.Base = Backbone.View.extend({
  initialize: function() {
    this.group = new Kinetic.Group();

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
  }
});

BattleArena.Models.Hero = Backbone.Model.extend({
});

BattleArena.Views.Hero = Backbone.View.extend({
  initialize: function() {
    this.group = new Kinetic.Group();

    var square = new Kinetic.Rect({
      x: this.model.get('x'),
      y: this.model.get('y'),
      width: this.model.get('width'),
      height: this.model.get('height'),
      stroke: 'yellow',
      fill: this.model.get('fill'),
      strokeWidth: 2
    });

    this.group.add(square);
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

    this.get('tiles').each(function(tile) {
      tile.get('objects').on('add', function(tileObject, tileObjects, options) {
        objectSpace.onTileObjectAdd(tile, tileObject, tileObjects, options);
      }, objectSpace);

      tile.get('objects').on('remove', function(tileObject, tileObjects, options) {
        objectSpace.onTileObjectRemove(tile, tileObject, tileObjects, options);
      }, objectSpace);
    });

    this.set('grid', new PF.Grid(
      BattleArena.Config.verticalTilesCount,
      BattleArena.Config.horizontalTilesCount
    ));
  },

  tilesOccupiedByObjectWithAttributes: function(x, y, width, height) {
    var minimumX = x - (x % BattleArena.Config.tileWidth);
    var minimumY = y - (y % BattleArena.Config.tileHeight);
    var maximumX = x +
                     width -
                     ((x + width) % BattleArena.Config.tileWidth);
    var maximumY = y +
                     height -
                     ((y + height) % BattleArena.Config.tileHeight);
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
  },

  onTileObjectAdd: function(tile, tileObject, tileObjects, options) {
  },

  onTileObjectRemove: function(tile, tileObject, tileObjects, options) {
  }
});

$(document).ready(function () {
  BattleArena.init();
});
