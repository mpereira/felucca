(ns battle-arena.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ;;[om.core :as om :include-macros true]
            ;;[om.dom :as dom :include-macros true]
            [clojure.set :as set]
            [clojure.zip :as zip]
            [clojure.string :as string]
            [cljs.core.async :refer [<! >! chan close! put! alts! timeout]]))

(enable-console-print!)

(let [stats (.Stats js/window)]
  (.setMode stats 0)
  (set! (.-position (.-style (.-domElement stats))) "fixed")
  (set! (.-left (.-style (.-domElement stats))) "0px")
  (set! (.-top (.-style (.-domElement stats))) "0px")
  (.appendChild (.-body js/document) (.-domElement stats))
  (js/setInterval #(.update stats) (/ 1000 60)))

(def configuration
  {:map {:vertical-tiles-count 40
         :horizontal-tiles-count 40}

   :tiles {:width 20
           :height 20
           :fill "#83AF9B"
           :stroke "#bbb"
           :stroke-width 1}

   :bases {:width 100
           :height 100
           :stroke-width 4
           :top {:fill "blue"
                 :stroke "#abc"}
           :bottom {:fill "red"
                    :stroke "#abc"}}

   :heroes #{{:name "Anti-Mage"
              :width 40
              :height 40
              :stroke "yellow"
              :stroke-width 2
              :fill "orange"
              :hit-points 530
              :mana 195
              :armor 2.08
              :movement-speed 315
              :attack {:speed 0.84
                       :range 128
                       :damage [49 53]}}
             {:name "Lion"
              :width 40
              :height 40
              :stroke "yellow"
              :stroke-width 2
              :fill "cyan"
              :hit-points 530
              :mana 195
              :armor 2.08
              :movement-speed 315
              :attack {:speed 0.84
                       :range 128
                       :damage [49 53]}}}

   :creeps #{{:name "Melee Creep"
              :width 20
              :height 20
              :stroke "yellow"
              :stroke-width 2
              :fill "green"
              :hit-points 550
              :mana 0
              :armor 2
              :movement-speed 325
              :gold [38 48]
              :experience 62
              :attack {:speed 0.5
                       :range 100
                       :damage [19 23]}}}

   :state-change-interval (/ 1000 60)

   :walkable-tile-fill "#83AF9B"
   :non-walkable-tile-fill "#ddd"
   :tile-click-highlight-fill "green"
   :tile-click-highlight-duration 500
   :tile-pathfinding-highlight-fill "#a4deb2"
   :should-highlight-occupied-tiles true
   :should-highlight-pathfinding true

   :melee-creep-width 40
   :melee-creep-height 40
   :melee-creep-fill "green"
   :melee-creep-movement-speed 0.5
   :melee-creep-minimum-strength 5
   :melee-creep-maximum-strength 50

   :creep-spawner-melee-creeps-count 1
   :creep-spawner-melee-creep-interval 30000

   :movement-handler-delay 10
   :attack-handler-delay 100})

(defn uuid [] (.v1 (.-uuid js/window)))

(defn tiles []
  (let [{:keys [vertical-tiles-count
                horizontal-tiles-count]} (:map configuration)
        {:keys [width
                height
                fill
                stroke
                stroke-width]} (get-in configuration [:tiles])]
    (for [x (range 0 (* vertical-tiles-count width) width)
          y (range 0 (* horizontal-tiles-count height) height)]
      {:id (uuid)
       :x x
       :y y
       :width width
       :height height
       :fill fill
       :stroke stroke
       :stroke-width stroke-width})))

(defn base [properties]
  (let [{:keys [width height stroke-width]} (:bases configuration)
        {:keys [x y fill stroke]} properties]
    {:id (uuid)
     :x x
     :y y
     :width width
     :height height
     :fill fill
     :stroke stroke
     :stroke-width stroke-width}))

(defn find-view [stage id]
  (.find stage (str "#" id)))

(defn tile-view [{:keys [path value]}]
  (let [{:keys [id x y width height fill stroke stroke-width]} value
        group (Kinetic.Group. #js {:id id :x x :y y :listening false})
        rectangle (Kinetic.Rect. #js {:width width
                                      :height height
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening false})]
    (.add group rectangle)
    group))

(defn tile-views [tiles] (map tile-view tiles))

(defn base-view [{:keys [path value]}]
  (let [{:keys [id x y width height fill stroke stroke-width]} value
        group (Kinetic.Group. #js {:id id :x x :y y :listening false})
        rectangle (Kinetic.Rect. #js {:width width
                                      :height height
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening false})]
    (.add group rectangle)
    group))

(defn base-views [bases] (map base-view bases))

(defn hero-view [{:keys [path value]}]
  (let [{:keys [id x y width height fill stroke stroke-width]} value
        group (Kinetic.Group. #js {:id id :x x :y y :listening false})
        rectangle (Kinetic.Rect. #js {:name "hero-rectangle"
                                      :width width
                                      :height height
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening false})]
    (.add group rectangle)
    group))

(defn hero-views [heroes] (map hero-view heroes))

(defn update-hero-view [stage hero]
  (let [{:keys [x y width height fill stroke stroke-width]} hero
        view (find-view stage (:id hero))
        hero-rectangle (.find view ".hero-rectangle")]
    (.setAttrs view #js {:x x :y y})
    (.setAttrs hero-rectangle #js {:width width
                                   :height height
                                   :fill "black"
                                   :stroke stroke
                                   :strokeWidth (rand 5)})
    view))

(defn creep-view [{:keys [path value]}]
  (let [{:keys [id x y width height fill stroke stroke-width]} value
        group (Kinetic.Group. #js {:id id :x x :y y :listening false})
        rectangle (Kinetic.Rect. #js {:width width
                                      :height height
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening false})]
    (.add group rectangle)
    group))

(defn creep-views [creeps] (map creep-view creeps))

(defn value-bar-view [] (Kinetic.Group. #js {:listening false}))
(defn value-bar-views [] (Kinetic.Group. #js {:listening false}))

(defn hero-with-name [heroes name]
  (first (set/select #(= (:name %) name) heroes)))

(defn creep-with-name [creeps name]
  (first (set/select #(= (:name %) name) creeps)))

(defn lane [tiles creeps]
  {:creeps creeps :tiles tiles})

(def state
  (let [
        vertical-tiles-count (get-in configuration [:map :vertical-tiles-count])
        horizontal-tiles-count (get-in configuration [:map :horizontal-tiles-count])
        tile-width (get-in configuration [:tiles :width])
        tile-height (get-in configuration [:tiles :height])
        base-width (get-in configuration [:bases :width])
        base-height (get-in configuration [:bases :height])
        dire-base (base (merge (get-in configuration [:bases :top])
                               {:x (- (* vertical-tiles-count tile-width)
                                      base-width
                                      tile-width)
                                :y tile-width}))
        radiant-base (base (merge (get-in configuration [:bases :bottom])
                                  {:x tile-width
                                   :y (- (* horizontal-tiles-count tile-height)
                                         base-height
                                         tile-height)}))
        anti-mage (hero-with-name (:heroes configuration) "Anti-Mage")
        lion (hero-with-name (:heroes configuration) "Lion")
        melee-creep (creep-with-name (:creeps configuration) "Melee Creep")
        dire-heroes {:anti-mage (merge anti-mage {:id (uuid)
                                                  :x (- (:x dire-base)
                                                        (+ (:width anti-mage)
                                                           tile-width))
                                                  :y (+ (:y dire-base)
                                                        base-height
                                                        tile-height)})}
        radiant-heroes {:lion (merge lion {:id (uuid)
                                           :x (+ (:x radiant-base)
                                                 base-width
                                                 tile-width)
                                           :y (- (:y radiant-base)
                                                 (+ (:width lion)
                                                    tile-height))})}
        dire-top-lane-creeps #{(merge melee-creep {:id (uuid)
                                                   :x (- (:x dire-base)
                                                         (* 2 tile-width))
                                                   :y (+ (:y dire-base)
                                                         (- (/ base-height 2)
                                                            (rem (/ base-height 2)
                                                                 tile-height)))})
                               (merge melee-creep {:id (uuid)
                                                   :x (- (:x dire-base)
                                                         (* 4 tile-width))
                                                   :y (+ (:y dire-base)
                                                         (- (/ base-height 2)
                                                            (rem (/ base-height 2)
                                                                 tile-height)))})
                               (merge melee-creep {:id (uuid)
                                                   :x (- (:x dire-base)
                                                         (* 6 tile-width))
                                                   :y (+ (:y dire-base)
                                                         (- (/ base-height 2)
                                                            (rem (/ base-height 2)
                                                                 tile-height)))})}
        dire-top-lane-tiles (map (fn [a b] {:x a :y b})
                                 (range (- (:x dire-base) (* 2 tile-width))
                                        (+ (:x radiant-base)
                                           (- (/ base-width 2)
                                              (rem (/ base-width 2) tile-width)))
                                        (- tile-width))
                                 (repeat (+ (:y dire-base)
                                            (- (/ base-height 2)
                                               (rem (/ base-height 2)
                                                    tile-height)))))
        dire-top-lane (lane dire-top-lane-tiles dire-top-lane-creeps)
        ]
    (atom {:teams {:dire {:name "Dire"
                          :keyword :dire
                          :position :top
                          :base dire-base
                          :heroes dire-heroes
                          :lanes {:top dire-top-lane
                                  :middle {}
                                  :bottom {}}}
                   :radiant {:name "Radiant"
                             :keyword :radiant
                             :position :bottom
                             :base radiant-base
                             :heroes radiant-heroes}}
           :map {:tiles (tiles)}
           :creep-spawners []
           :objects []})))

(defn map-zip [m]
  (zip/zipper #(or (map? %) (map? (nth % 1)))
              #(seq (if (map? %) % (nth % 1)))
              (fn [x children]
                (if (map? x)
                  (into {} children)
                  (assoc x 1 (into {} children))))
              m))

(defn path-in-state [state value]
  (loop [current-location (map-zip state)]
    (let [current-node (zip/node current-location)
          current-path (zip/path current-location)]
      (if (= (last current-node) (:id value))
        (into [] (rest (map first current-path)))
        (if (zip/end? current-location)
          nil
          (recur (zip/next current-location)))))))

(defn movement-speed [hero]
  1)

(defn to-cursor [state value]
  {:path (path-in-state state value) :value value})

(defn move-towards [state hero tile]
  (update-in state
             (path-in-state state hero)
             merge
             {:destination (select-keys tile [:x :y])}))

(defn move-towards! [hero-path tile]
  (swap! state move-towards hero-path tile))

(def stage
  (Kinetic.Stage. #js {:container "canvas-container"
                       :width 800
                       :height 800
                       :listening false}))

(def layers
  (sorted-map :tiles (Kinetic.Layer. #js {:listening false})
              :bases (Kinetic.Layer. #js {:listening false})
              :creeps (Kinetic.Layer. #js {:listening false})
              :heroes (Kinetic.Layer. #js {:listening false})
              :value-bars (Kinetic.Layer. #js {:listening false})))

(let [tiles (get-in @state [:map :tiles])
      bases (map :base (vals (:teams @state)))
      heroes (flatten (map vals (map :heroes (vals (:teams @state)))))
      creeps (reduce
               set/union
               (map #(get-in % [:lanes :top :creeps]) (vals (:teams @state))))]
  (doseq [v (tile-views (map (partial to-cursor @state) tiles))] (.add (:tiles layers) v))
  (doseq [v (base-views (map (partial to-cursor @state) bases))] (.add (:bases layers) v))
  (doseq [v (hero-views (map (partial to-cursor @state) heroes))] (.add (:heroes layers) v))
  (doseq [v (creep-views (map (partial to-cursor @state) creeps))] (.add (:creeps layers) v))
  (.add (:value-bars layers) (value-bar-views)))

(doseq [layer (reverse (vals layers))] (.add stage layer))

(defn next-hero-state [hero]
  (when-let [destination (:destination hero)]
    (let [delta-x (- (:x destination) (:x hero))
          delta-y (- (:y destination) (:y hero))]
      (merge-with +
                  hero
                  {:x (cond
                        (pos? delta-x) (movement-speed hero)
                        (neg? delta-x) (- (movement-speed hero))
                        :else 0)
                   :y (cond
                        (pos? delta-y) (movement-speed hero)
                        (neg? delta-y) (- (movement-speed hero))
                        :else 0)}))))

(defn next-state [state]
  (-> state
      (update-in [:teams :radiant :heroes :lion] next-hero-state)
      (update-in [:teams :dire :heroes :anti-mage] next-hero-state)))

(defn next-state! [state]
  (swap! state next-state))

(defn update-stage! [stage state]
  (update-hero-view stage (get-in state [:teams :radiant :heroes :lion]))
  (update-hero-view stage (get-in state [:teams :dire :heroes :anti-mage])))

(defn draw-stage! [stage]
  (.draw (:heroes layers)))

(move-towards! (get-in @state [:teams :radiant :heroes :lion]) {:x 500 :y 50})
(move-towards! (get-in @state [:teams :dire :heroes :anti-mage]) {:x 50 :y 500})

(go
  (loop [current-state @state]
    (update-stage! stage current-state)
    (draw-stage! stage)
    (<! (timeout (:state-change-interval configuration)))
    (recur (next-state! state))))

;; (prn @state)
;; (prn (get-in (first (get-in @state [:teams])) [:lanes :top]))
