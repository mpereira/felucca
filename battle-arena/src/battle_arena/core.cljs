(ns battle-arena.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [clojure.set :as set]
            [clojure.zip :as zip]
            [clojure.string :as string]
            [cljs.core.async :refer [<! >! chan close! put! alts! timeout]]
            [battle-arena.geometry :refer [point-in-triangle?
                                           point-in-rectangle?
                                           point-distance]]))

(repl/connect "http://localhost:9000/repl")

(enable-console-print!)

;; Helpers.

(defn uuid [] (.v1 (.-uuid js/window)))

(defn log [value] (.log js/console value))

(defn index-of
  "Returns the index of a value in a seq."
  ([s value] (index-of s value 0))
  ([s value index]
   (when-not (empty? s)
     (if (= value (first s)) index (recur (rest s) value (inc index))))))

(defn cljs->js
  "Recursively transforms ClojureScript maps into Javascript objects, other
  ClojureScript colls into JavaScript arrays, and ClojureScript keywords into
  JavaScript strings."
  [x]
  (cond
    (string? x) x
    (keyword? x) (name x)
    (map? x) (apply js-obj (flatten (map (fn [[k v]] [(cljs->js k) (cljs->js v)]) x)))
    (coll? x) (apply array (map cljs->js x))
    :else x))

;; Tree path finding and cursor-related functions.

(defn heterogeneous-zipper [m]
  (zip/zipper #(constantly true)
              #(seq (cond (map? %) (vals %) (vector? %) % :else nil))
              #(constantly %)
              m))

(defn heterogeneous-path
  ([zipper-path value] (heterogeneous-path (reverse zipper-path) value []))
  ([zipper-path value path]
   (if (empty? zipper-path)
     (reverse path)
     (let [next-value (first zipper-path)
           value-key (cond
                       (vector? next-value) (index-of next-value value)
                       (map? next-value) (get (set/map-invert next-value) value))]
       (recur (rest zipper-path) next-value (conj path value-key))))))

(defn path [state value]
  (loop [current-location (heterogeneous-zipper state)]
    (let [current-node (zip/node current-location)
          current-path (zip/path current-location)]
      (if (= (:id current-node) (:id value))
        (heterogeneous-path current-path current-node)
        (if (zip/end? current-location)
          nil
          (recur (zip/next current-location)))))))

(defn to-cursor [state value]
  {:state state :path (path @state value)})

;; Mechanics.

(def configuration
  {:map {:vertical-tiles-count 40
         :horizontal-tiles-count 40}

   :tiles {:dimensions {:width 20 :height 20}
           :fill "#83AF9B"
           :stroke "#bbb"
           :stroke-width 1}

   :bases {:dimensions {:width 100 :height 100}
           :stroke-width 4
           :top {:fill "blue"
                 :stroke "#abc"}
           :bottom {:fill "red"
                    :stroke "#abc"}}

   :lanes {:tiles {:dimensions {:width 20 :height 20}
                   :fill "#BACFC5"
                   :stroke "#ddd"
                   :stroke-width 1}}

   :heroes #{{:name "Anti-Mage"
              :dimensions {:width 40 :height 40}
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
              :dimensions {:width 40 :height 40}
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
              :dimensions {:width 20 :height 20}
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

   :state-change-interval (/ 1000 60)})

(defn movement-speed [creature]
  (/ (:movement-speed creature) 300))

(defn distance [a b]
  (apply point-distance (map (juxt :x :y) (map :coordinates [a b]))))

(defn to-point [thing]
  ((juxt :x :y) (:coordinates thing)))

(defn to-rectangle [thing]
  (apply conj (to-point thing) ((juxt :width :height) (:dimensions thing))))

(defn creep-within-tile? [creep tile]
  (point-in-rectangle? (to-point creep) (to-rectangle tile)))

(defn creep-inside-two-subsequent-tiles-path?
  [creep [t0 t1 :as tiles]]
  (let [tiles-coordinates (map :coordinates tiles)
        cx (:x (:coordinates creep))
        cy (:y (:coordinates creep))
        t0x (:x (get-in t0 [:coordinates :x]))
        t0y (:y (get-in t0 [:coordinates :y]))
        t1x (:x (get-in t1 [:coordinates :x]))
        t1y (:y (get-in t1 [:coordinates :y]))]
    (or (some (partial creep-within-tile? creep) tiles)
        (if (= (- t0x t1x) (- t0y t1y)) ;; -45 degrees
          (or (point-in-triangle? [cx cy]
                                  [[(max t0x t1x) (min t0y t1y)]
                                   [(max t0x t1x) (max t0y t1y)]
                                   [(+ (max t0x t1x) (Math/abs (- t0x t1x)))
                                    (max t0y t1y)]])
              (point-in-triangle? [cx cy]
                                  [[(min t0x t1x) (max t0y t1y)]
                                   [(max t0x t1x) (max t0y t1y)]
                                   [(max t0x t1x)
                                    (+ (max t0y t1y) (Math/abs (- t0y t1y)))]]))
          (or (point-in-triangle? [cx cy]
                                  [[(min t0x t1x) (max t0y t1y)]
                                   [(max t0x t1x) (max t0y t1y)]
                                   [(max t0x t1x) (min t0y t1y)]])
              (point-in-triangle? [cx cy]
                                  [[(max t0x t1x)
                                    (+ (max t0y t1y) (Math/abs (- t0y t1y)))]
                                   [(max t0x t1x) (max t0y t1y)]
                                   [(+ (max t0x t1x) (Math/abs (- t0x t1x)))
                                    (max t0y t1y)]]))))))

(defn creep-within-lane? [creep {:keys [tiles]}]
  (some #(creep-inside-two-subsequent-tiles-path? creep %)
        (partition 2 1 tiles)))

(defn lane-tile-below-creep [{:keys [tiles]} creep]
  (first (filter (partial creep-within-tile? creep) tiles)))

;; FIXME improve performance.
(defn tile-closest-to-creep [tiles creep]
  (first (sort-by (partial distance creep) tiles)))

(defn lane-tile-closest-to-creep [lane creep]
  (first (sort-by (partial distance creep) (:tiles lane))))

(defn closest-lane-tile [{:keys [tiles]} tile]
  (first (sort-by (partial distance tile) tiles)))

(defn hero-with-name [heroes name]
  (first (set/select #(= (:name %) name) heroes)))

(defn creep-with-name [creeps name]
  (first (set/select #(= (:name %) name) creeps)))

(defn tile-coordinates-from-to [from-coordinates to-coordinates]
  (let [x-sign (if (> (:x to-coordinates) (:x from-coordinates)) + -)
        y-sign (if (> (:y to-coordinates) (:y from-coordinates)) + -)]
    (map (fn [x y _] {:x x :y y})
         (lazy-cat (range (:x from-coordinates)
                          (x-sign (:x to-coordinates)
                                  (get-in configuration [:tiles :dimensions :width]))
                          (x-sign (get-in configuration [:tiles :dimensions :width])))
                   (repeat (:x to-coordinates)))
         (lazy-cat (range (:y from-coordinates)
                          (y-sign (:y to-coordinates)
                                  (get-in configuration [:tiles :dimensions :height]))
                          (y-sign (get-in configuration [:tiles :dimensions :height])))
                   (repeat (:y to-coordinates)))
         (range (inc (max (/ (Math/abs (- (:x to-coordinates) (:x from-coordinates)))
                             (get-in configuration [:tiles :dimensions :width]))
                          (/ (Math/abs (- (:y to-coordinates) (:y from-coordinates)))
                             (get-in configuration [:tiles :dimensions :height]))))))))

(defn path-between-objects [a b]
  (map (juxt :x :y) (tile-coordinates-from-to (:coordinates (lane-tile-below-creep a)) (:coordinates b))))

(defn tiles-to-the-top [thing n]
  (assoc-in thing
            [:coordinates :y]
            (- (get-in thing [:coordinates :y])
               (* n (get-in configuration [:tiles :dimensions :height])))))

(defn tiles-to-the-bottom [thing n]
  (assoc-in thing
            [:coordinates :y]
            (+ (get-in thing [:coordinates :y])
               (* n (get-in configuration [:tiles :dimensions :height])))))

(defn tiles-to-the-right [thing n]
  (assoc-in thing
            [:coordinates :x]
            (+ (get-in thing [:coordinates :x])
               (* n (get-in configuration [:tiles :dimensions :width])))))

(defn tiles-to-the-left [thing n]
  (assoc-in thing
            [:coordinates :x]
            (- (get-in thing [:coordinates :x])
               (* n (get-in configuration [:tiles :dimensions :width])))))

(defn tile-with-coordinates [ts c]
  (first (filter #(= (:coordinates %) c) ts)))

(defn tiles-with-coordinates [ts cs]
  (map (partial tile-with-coordinates ts) cs))

;; Computing the next state.

(defn next-lane-tile [{:keys [tiles] :as lane} creep]
  (let [t (lane-tile-below-creep lane creep)]
    (if (= (:coordinates t) (:coordinates (last tiles)))
      (last tiles)
      (second (drop-while #(not= (:coordinates t) (:coordinates %)) tiles)))))

(defn next-lane-creep-destination [lane creep]
  (:coordinates (if (creep-within-lane? creep lane)
                  (next-lane-tile lane creep)
                  (closest-lane-tile lane creep))))

(defn next-coordinates-delta [creature]
  (let [x0 (get-in creature [:coordinates :x])
        x1 (get-in creature [:destination :x])
        y0 (get-in creature [:coordinates :y])
        y1 (get-in creature [:destination :y])
        dx (- x1 x0)
        dy (- y1 y0)
        scale (Math/sqrt (+ (Math/pow dx 2) (Math/pow dy 2)))
        v (movement-speed creature)
        ndx (* v (/ dx scale))
        ndy (* v (/ dy scale))]
    {:x (cond
          (pos? dx) (if (> (+ x0 ndx) x1) dx ndx)
          (neg? dx) (if (< (+ x0 ndx) x1) dx ndx)
          :else 0)
     :y (cond
          (pos? dy) (if (> (+ y0 ndy) y1) dy ndy)
          (neg? dy) (if (< (+ y0 ndy) y1) dy ndy)
          :else 0)}))

(defn next-coordinates [creature]
  (merge-with + (:coordinates creature) (next-coordinates-delta creature)))

(defn next-hero-state [hero]
  (if-let [destination (:destination hero)]
    (merge hero
           (if (= (:coordinates destination) (:coordinates hero))
             {:destination nil}
             {:coordinates (next-coordinates hero)}))
    hero))

(defn next-creep-state [creep]
  (if-let [destination (:destination creep)]
    (merge creep
           (if (= (:coordinates destination) (:coordinates creep))
             {:destination nil}
             {:coordinates (next-coordinates creep)}))
    creep))

(defn next-creeps-state [creeps]
  (into [] (map next-creep-state creeps)))

(defn next-lane-creep-state [lane creep]
  (next-creep-state (merge creep
                           {:destination (next-lane-creep-destination lane
                                                                      creep)})))

(defn next-lane-creeps-state [lane creeps]
  (into [] (map (partial next-lane-creep-state lane) creeps)))

(defn next-lane-state [{:keys [tiles creeps] :as lane}]
  {:tiles tiles
   :creeps (next-lane-creeps-state lane creeps)})

(defn next-team-state [team])
(defn next-teams-state [teams] (map next-team-state teams))

(defn next-state [state]
  (-> state
      (update-in [:teams :dire :lanes :top] next-lane-state)
      (update-in [:teams :radiant :heroes :lion] next-hero-state)
      (update-in [:teams :dire :heroes :anti-mage] next-hero-state)))

;; State mutation.

(defn move-towards! [state hero tile]
  (swap! state
         (fn [state hero tile]
           (update-in state
                      (path state hero)
                      merge
                      {:destination (:coordinates tile)
                       :trail {:id (uuid)
                               :coordinates (:coordinates hero)
                               :points (path-between-objects hero tile)}}))
         hero
         tile))

(defn next-state! [state]
  (swap! state next-state))

;; Views.

(defn find-view [view selector] (.find view selector))
(defn find-view-by-id [view id] (find-view view (str "#" id)))
(defn find-view-by-name [view name] (find-view view (str "." name)))

(defn hit-points-bar-view [{:keys [path state]}]
  (let [value (get-in @state path)
        {:keys [coordinates dimensions]} value
        width (* 0.75 (:width dimensions))
        group (Kinetic.Group. #js {:x (:x coordinates)
                                   :y (- (:y coordinates) 20)
                                   :listening false})
        rectangle (Kinetic.Rect. #js {:width width
                                      :height 20
                                      :fill "red"
                                      :stroke "black"
                                      :strokeWidth 2
                                      :listening false})]
    (.add group rectangle)
    group))

(defn tile-view [{:keys [path state]}]
  (let [value (get-in @state path)
        {:keys [id coordinates dimensions fill stroke stroke-width]} value
        group (Kinetic.Group. #js {:id id
                                   :x (:x coordinates)
                                   :y (:y coordinates)
                                   :listening true})
        rectangle (Kinetic.Rect. #js {:width (:width dimensions)
                                      :height (:height dimensions)
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening true})]
    (.add group rectangle)
    group))

(defn tile-views [cursors] (map tile-view cursors))

(defn base-view [{:keys [path state] :as cursor}]
  (let [value (get-in @state path)
        {:keys [id coordinates dimensions fill stroke stroke-width]} value
        group (Kinetic.Group. #js {:id id
                                   :x (:x coordinates)
                                   :y (:y coordinates)
                                   :listening false})
        rectangle (Kinetic.Rect. #js {:width (:width dimensions)
                                      :height (:height dimensions)
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening false})]
    (.add group rectangle)
    ;; TODO: hit-points-bar-view needs to be injected in the bar-views layer.
    (.add group (hit-points-bar-view cursor))
    group))

(defn lane-view [{:keys [path state]}]
  (let [value (get-in @state path)
        outer-group (Kinetic.Group.)]
    (doseq [{:keys [id coordinates dimensions fill stroke stroke-width]} (:tiles value)]
      (let [group (Kinetic.Group. #js {:id id
                                       :x (:x coordinates)
                                       :y (:y coordinates)
                                       :listening false})
            rectangle (Kinetic.Rect. #js {:width (:width dimensions)
                                          :height (:height dimensions)
                                          :fill "#BACFC5"
                                          :stroke "#ddd"
                                          :strokeWidth stroke-width
                                          :listening false})]
        (.add group rectangle)
        (.add outer-group group)))
    outer-group))

(defn lane-views [cursors] (map lane-view cursors))

(defn base-views [cursors] (map base-view cursors))

(defn hero-view [{:keys [path state]}]
  (let [value (get-in @state path)
        {:keys [id coordinates dimensions fill stroke stroke-width]}
        group (Kinetic.Group. #js {:id id
                                   :x (:x coordinates)
                                   :y (:y coordinates)
                                   :listening true})
        rectangle (Kinetic.Rect. #js {:name "hero-rectangle"
                                      :width (:width dimensions)
                                      :height (:height dimensions)
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening true})]
    (.add group rectangle)
    group))

(defn hero-views [cursors] (map hero-view cursors))

(defn path-view [{:keys [path state]}]
  (let [value (get-in @state path)
        {:keys [id coordinates points]} value
        group (Kinetic.Group. #js {:id id
                                   :x (:x coordinates)
                                   :y (:y coordinates)
                                   :listening false})
        line (Kinetic.Line. #js {:name "line"
                                 :points #js []
                                 :stroke "red"
                                 :strokeWidth 3})]
    (.add group rectangle line)
    group))

(defn path-views [cursors] (map path-view cursors))

(defn update-path-view! [canvas path]
  (let [view (or (first (find-view-by-id canvas (:id path)))o
                 (path-view [] path))
        {:keys [coordinates]} path
        line (first (find-view-by-name view "line"))]
    (.setAttrs view #js {:x (:x coordinates) :y (:y coordinates)})
    (.setAttrs hero-rectangle #js {:width (:width dimensions)
                                   :height (:height dimensions)
                                   :fill fill
                                   :stroke stroke
                                   :strokeWidth stroke-width})
    (.setAttrs line #js {:points (cljs->js (flatten path))})
    view))

(defn update-hero-view! [canvas hero]
  (let [{:keys [coordinates dimensions fill stroke stroke-width trail]} hero
        view (first (find-view-by-id canvas (:id hero)))
        hero-rectangle (first (find-view-by-name view "hero-rectangle"))
        path-line (first (find-view-by-name view "hero-path-line"))]
    (.setAttrs view #js {:x (:x coordinates) :y (:y coordinates)})
    (.setAttrs hero-rectangle #js {:width (:width dimensions)
                                   :height (:height dimensions)
                                   :fill fill
                                   :stroke stroke
                                   :strokeWidth stroke-width})
    (.setAttrs path-line #js {:points (cljs->js (flatten trail))})
    view))

(defn update-creep-view! [canvas creep]
  (let [{:keys [coordinates dimensions fill stroke stroke-width]} creep
        view (first (find-view-by-id canvas (:id creep)))
        creep-rectangle (first (find-view-by-name view "creep-rectangle"))]
    (.setAttrs view #js {:x (:x coordinates) :y (:y coordinates)})
    (.setAttrs creep-rectangle #js {:width (:width dimensions)
                                    :height (:height dimensions)
                                    :fill fill
                                    :stroke stroke
                                    :strokeWidth stroke-width})
    view))

(defn update-creep-views! [canvas creeps]
  (doseq [creep creeps] (update-creep-view! canvas creep)))

(defn creep-view [{:keys [path state]}]
  (let [value (get-in @state path)
        {:keys [id coordinates dimensions fill stroke stroke-width]} value
        group (Kinetic.Group. #js {:id id
                                   :x (:x coordinates)
                                   :y (:y coordinates)
                                   :listening false})
        rectangle (Kinetic.Rect. #js {:name "creep-rectangle"
                                      :width (:width dimensions)
                                      :height (:height dimensions)
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening false})]
    (.add group rectangle)
    group))

(defn creep-views [cursors] (map creep-view cursors))

;; UI.

;; TODO: make group/layer constructors automatically compute their coordinates
;; and dimensions based on their children.

(defn canvas [options] (Kinetic.Stage. (cljs->js options)))
(defn layer [options] (Kinetic.Layer. (cljs->js options)))

(defn add-layer [canvas layer] (.add canvas layer))
(defn add-layers [canvas layers] (doseq [layer layers] (add-layer canvas layer)))

(defn add-view [layer view] (.add layer view))
(defn add-views [layer views] (doseq [view views] (add-view layer view)))

(defn cache [shape]
  (.cache shape (cljs->js {:x (.x shape)
                           :y (.y shape)
                           :width (.width shape)
                           :height (.height shape)})))

(defn update-canvas! [canvas previous-state current-state]
  (update-hero-view! canvas (get-in current-state [:teams :radiant :heroes :lion]))
  (update-path-view! canvas (get-in current-state [:teams :radiant :heroes :lion :trail]))
  (update-hero-view! canvas (get-in current-state [:teams :dire :heroes :anti-mage]))
  (update-creep-views! canvas (get-in current-state [:teams :dire :lanes :top :creeps])))

(defn draw-canvas! [canvas] (doseq [layer (.getLayers canvas)] (.draw layer)))

;;

(defn tiles []
  (let [{:keys [vertical-tiles-count horizontal-tiles-count]} (:map configuration)
        {:keys [dimensions
                fill
                stroke
                stroke-width]} (get-in configuration [:tiles])]
    (into []
          (for [x (range 0
                         (* vertical-tiles-count (:width dimensions))
                         (:width dimensions))
                y (range 0
                         (* horizontal-tiles-count (:height dimensions))
                         (:height dimensions))]
            {:id (uuid)
             :coordinates {:x x :y y}
             :dimensions dimensions
             :fill fill
             :stroke stroke
             :stroke-width stroke-width}))))

(defn base [properties]
  (let [{:keys [dimensions stroke-width]} (:bases configuration)
        {:keys [coordinates fill stroke]} properties]
    {:id (uuid)
     :coordinates coordinates
     :dimensions dimensions
     :fill fill
     :stroke stroke
     :stroke-width stroke-width}))

(defn lane [tiles creeps]
  {:id (uuid) :creeps creeps :tiles tiles})

(let [camera {:x 0 :y 0 :width (* 40 20) :height (* 40 20)}]
  (def ui {:canvas (canvas {:container "canvas-container"
                            :width 800
                            :height 800
                            :listening false})
           ;; FIXME actually implement a map sorted by z-index.
           :layers (sorted-map :tiles (layer (merge camera {:listening true}))
                               :bases (layer (merge camera {:listening false}))
                               :lanes (layer (merge camera {:listening false}))
                               :paths (layer (merge camera {:listening false})) 
                               :creeps (layer (merge camera {:listening false}))
                               :heroes (layer (merge camera {:listening true}))
                               :bars (layer (merge camera {:listening false})))
           :camera camera}))

(def state
  (let [vertical-tiles-count (get-in configuration [:map :vertical-tiles-count])
        horizontal-tiles-count (get-in configuration [:map :horizontal-tiles-count])
        tile-width (get-in configuration [:tiles :dimensions :width])
        tile-height (get-in configuration [:tiles :dimensions :height])
        map-tiles (tiles)
        base-width (get-in configuration [:bases :dimensions :width])
        base-height (get-in configuration [:bases :dimensions :height])
        dire-base (base (assoc (get-in configuration [:bases :top])
                               :coordinates
                               {:x (- (* vertical-tiles-count tile-width)
                                      base-width
                                      tile-width)
                                :y tile-width}))
        radiant-base (base (assoc (get-in configuration [:bases :bottom])
                                  :coordinates
                                  {:x tile-width
                                   :y (- (* horizontal-tiles-count tile-height)
                                         base-height
                                         tile-height)}))
        anti-mage (hero-with-name (:heroes configuration) "Anti-Mage")
        lion (hero-with-name (:heroes configuration) "Lion")
        melee-creep (creep-with-name (:creeps configuration) "Melee Creep")
        dire-heroes {:anti-mage (merge anti-mage
                                       {:id (uuid)
                                        :coordinates
                                        {:x (- (get-in dire-base [:coordinates :x])
                                               (+ (get-in anti-mage [:dimensions :width])
                                                  tile-width))
                                         :y (+ (get-in dire-base [:coordinates :y])
                                               base-height
                                               tile-height)}})}
        radiant-heroes {:lion (merge lion
                                     {:id (uuid)
                                      :coordinates
                                      {:x (+ (get-in radiant-base [:coordinates :x])
                                             base-width
                                             tile-width)
                                       :y (- (get-in radiant-base [:coordinates :y])
                                             (+ (get-in lion [:dimensions :width])
                                                tile-height))}})}
        dire-top-lane-creeps [(merge melee-creep
                                     {:id (uuid)
                                      :coordinates
                                      {:x (- (get-in dire-base [:coordinates :x])
                                             (* 2 tile-width))
                                       :y (+ (get-in dire-base [:coordinates :y])
                                             (- (/ base-height 2)
                                                (rem (/ base-height 2)
                                                     tile-height)))}})
                              (merge melee-creep
                                     {:id (uuid)
                                      :coordinates
                                      {:x (- (get-in dire-base [:coordinates :x])
                                             (* 4 tile-width))
                                       :y (+ (get-in dire-base [:coordinates :y])
                                             (- (/ base-height 2)
                                                (rem (/ base-height 2)
                                                     tile-height)))}})
                              (merge melee-creep
                                     {:id (uuid)
                                      :coordinates
                                      {:x (- (get-in dire-base [:coordinates :x])
                                             (* 6 tile-width))
                                       :y (+ (get-in dire-base [:coordinates :y])
                                             (- (/ base-height 2)
                                                (rem (/ base-height 2)
                                                     tile-height)))}})]
        dire-top-lane-origin-coordinates (:coordinates
                                           (tiles-to-the-bottom
                                             (tiles-to-the-left dire-base 2)
                                             2))
        dire-top-lane-destination-coordinates (:coordinates
                                                (tiles-to-the-top
                                                  (tiles-to-the-right radiant-base 2)
                                                  2))
        dire-top-lane-middle-coordinates {:x (:x dire-top-lane-destination-coordinates)
                                          :y (:y dire-top-lane-origin-coordinates)}
        dire-top-lane-coordinates (distinct
                                    (concat
                                      (tile-coordinates-from-to
                                        dire-top-lane-origin-coordinates
                                        dire-top-lane-middle-coordinates)
                                      (tile-coordinates-from-to
                                        dire-top-lane-middle-coordinates
                                        dire-top-lane-destination-coordinates)))
        dire-top-lane (lane (tiles-with-coordinates map-tiles dire-top-lane-coordinates)
                            dire-top-lane-creeps)]
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
           :map {:tiles map-tiles}})))

(let [tiles (get-in @state [:map :tiles])
      bases (map :base (vals (:teams @state)))
      heroes (flatten (map vals (map :heroes (vals (:teams @state)))))
      lanes (filter identity (flatten (map vals (map :lanes (vals (:teams @state))))))
      teams (:teams @state)
      dire-creeps (get-in (:dire teams) [:lanes :top :creeps])
      radiant-creeps (get-in (:radiant teams) [:lanes :top :creeps])
      creeps (concat dire-creeps radiant-creeps)]
  (add-views (get-in ui [:layers :tiles])
             ;; FIXME Manually creating cursors because calling `to-cursor`
             ;; for thousands of tiles is slow.
             (tile-views (map (fn [index]
                                {:path (conj [:map :tiles] index) :state state})
                              (range (count tiles)))))
  (add-views (get-in ui [:layers :bases]) (base-views (map (partial to-cursor state) bases)))
  (add-views (get-in ui [:layers :lanes]) (lane-views (map (partial to-cursor state) lanes)))
  (add-views (get-in ui [:layers :heroes]) (hero-views (map (partial to-cursor state) heroes)))
  (add-views (get-in ui [:layers :creeps]) (creep-views (map (partial to-cursor state) creeps)))
  (cache (get-in ui [:layers :tiles]))
  (cache (get-in ui [:layers :lanes]))
  (cache (get-in ui [:layers :bases]))
  (add-layers (:canvas ui) (reverse (vals (:layers ui)))))

;; (move-towards! state
;;                      (get-in @state [:teams :radiant :heroes :lion])
;;                      {:coordinates {:x 500 :y 50}})
;; (move-towards! state
;;                      (get-in @state [:teams :dire :heroes :anti-mage])
;;                      {:coordinates {:x 50 :y 500}})

(def tick-chan (chan))

(defn tick [] (put! tick-chan 1))

(doseq [t [["1-tick" 1]
           ["5-ticks" 5]
           ["10-ticks" 10]
           ["25-ticks" 25]
           ["50-ticks" 50]
           ["100-ticks" 100]
           ["1000-ticks" 1000]]]
  (.addEventListener (.getElementById js/document (nth t 0))
                     "click"
                     #(do
                        (.preventDefault %)
                        (dorun (repeatedly (nth t 1) tick)))))

(.addEventListener (.getContainer (:canvas ui))
                   "contextmenu"
                   #(.preventDefault %)
                   false)

(.on (get-in ui [:layers :tiles])
     "mouseup tap"
     #(let [event (.-evt %)
            lion (get-in @state [:teams :radiant :heroes :lion])]
        (.preventDefault event)
        (log "tile click!")
        (set! (.-tileClick js/window) event)
        (move-towards! state
                       lion
                       {:coordinates {:x (- (.-layerX event)
                                            (/ (get-in lion [:dimensions :width])
                                               2))
                                      :y (- (.-layerY event)
                                            (/ (get-in lion [:dimensions :height])
                                               2))}})))

(.on (get-in ui [:layers :heroes])
     "dblclick dbltap"
     #(do
        (.preventDefault %)
        (log "hero click!")
        (set! (.-heroClick js/window) %)))

(def grid
  (PF/Grid. (get-in configuration [:map :vertical-tiles-count])
            (get-in configuration [:map :horizontal-tiles-count])))

(def pathfinder
  (PF/AStarFinder. #js {:allowDiagonal true :dontCrossCorners true}))

(def grid-path (.findPath pathfinder 0 0 10 10 (.clone grid)))

(defn grid [tiles objects])

;; (.log js/console grid-path)

;; (.addEventListener (.getContainer (:canvas ui))
;;                    "click"
;;                    #(do (set! (.-foo js/window) %) (.preventDefault %) (log %)))

(def fpsmeter (js/FPSMeter.))

(prn
  (let [hero (get-in @state [:teams :radiant :heroes :lion])]
    (select-keys hero [:coordinates :destination :trail])))
;;
;; (log
;;   (let [lane (get-in @state [:teams :dire :lanes :top])
;;         creep (get-in @state [:teams :dire :lanes :top :creeps 0])]
;;     (to-rectangle (first (:tiles lane)))))

;;
;; (log
;;   (let [lane (get-in @state [:teams :dire :lanes :top])
;;         creep (get-in @state [:teams :dire :lanes :top :creeps 1])]
;;     (some (partial creep-within-tile? creep) (:tiles lane))))
;;
;; (log
;;   (let [lane (get-in @state [:teams :dire :lanes :top])
;;         creep (get-in @state [:teams :dire :lanes :top :creeps 1])]
;;     (:tiles lane)))
;;
;; (println
;;   (tiles-with-coordinates (get-in @state [:map :tiles]) [{:x 20 :y 0}]))

(go
  (loop [previous-state @state current-state @state]
    ;; (<! tick-chan)
    (<! (timeout (:state-change-interval configuration)))
    (update-canvas! (:canvas ui) previous-state current-state)
    (draw-canvas! (:canvas ui))
    (.tick fpsmeter)
    (recur current-state (next-state! state))))
