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
    baseWidth: 60,
    baseHeight: 60
  },
  init: function () {
    this.stage = new Kinetic.Stage({
      container: 'container',
      width: 640,
      height: 640
    });

    var map = new BattleArena.Models.Map({
      width: this.Config.verticalTilesCount * this.Config.tileWidth,
      height: this.Config.horizontalTilesCount * this.Config.tileHeight,
      tileWidth: this.Config.tileWidth,
      tileHeight: this.Config.tileHeight
    });

    var mapView = new BattleArena.Views.Map({ model: map });
    mapView.render();

    var bottomBase = new BattleArena.Models.Base({
      x: this.Config.tileWidth,
      y: (this.Config.horizontalTilesCount * this.Config.tileHeight) -
           this.Config.baseHeight -
           this.Config.tileHeight,
      width: this.Config.baseWidth,
      height: this.Config.baseHeight,
      color: 'red'
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
      color: 'blue'
    });

    var topBaseView = new BattleArena.Views.Base({ model: topBase });
    topBaseView.render()

    this.mapLayer = new Kinetic.Layer();
    this.basesLayer = new Kinetic.Layer();

    this.mapLayer.add(mapView.group);
    this.basesLayer.add(bottomBaseView.group);
    this.basesLayer.add(topBaseView.group);

    this.stage.add(this.mapLayer);
    this.stage.add(this.basesLayer);
  }
};

BattleArena.Models.Tile = Backbone.Model.extend({
});

BattleArena.Collections.Tiles = Backbone.Collection.extend({
});

BattleArena.Views.Tile = Backbone.View.extend({
  initialize: function() {
    this.group = new Kinetic.Group();

    var square = new Kinetic.Rect({
      x: this.model.get('x'),
      y: this.model.get('y'),
      width: this.model.get('width'),
      height: this.model.get('height'),
      stroke: '#ccc',
      strokeWidth: 2
    });

    this.group.add(square);
  },

  render: function() {
  }
});


BattleArena.Views.Tiles = Backbone.Marionette.CollectionView.extend({
  itemView: BattleArena.Views.Tile,

  initialize: function() {
    this.group = new Kinetic.Group();
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
  },
});

BattleArena.Views.Map = Backbone.View.extend({
  initialize: function() {
    this.group = new Kinetic.Group();

    this.tilesView = new BattleArena.Views.Tiles({
      collection: this.model.get('tiles')
    });

    this.group.add(this.tilesView.group);
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
      fill: this.model.get('color'),
      strokeWidth: 4
    });

    this.group.add(square);
  },

  render: function() {
  }
});

$(document).ready(function () {
  BattleArena.init();
});
