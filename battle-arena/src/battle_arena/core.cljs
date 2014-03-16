(ns battle-arena.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [clojure.set :as set]
            [clojure.string :as string]
            [cljs.core.async :refer [<! >! chan close! put! alts! timeout]]
            [battle-arena.state :as state :refer [to-cursor next-state!]]
            [battle-arena.ui :as ui]))

(repl/connect "http://localhost:9000/repl")

(enable-console-print!)

(defn uuid [] (.v1 (.-uuid js/window)))

(defn log [value]
  (.log js/console (if (and value (.-toString value)) (.toString value) value)))

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

   :state-change-interval (/ 1000 60)})

(defn tiles []
  (let [{:keys [vertical-tiles-count
                horizontal-tiles-count]} (:map configuration)
        {:keys [width
                height
                fill
                stroke
                stroke-width]} (get-in configuration [:tiles])]
    (into []
          (for [x (range 0 (* vertical-tiles-count width) width)
                y (range 0 (* horizontal-tiles-count height) height)]
            {:id (uuid)
             :x x
             :y y
             :width width
             :height height
             :fill fill
             :stroke stroke
             :stroke-width stroke-width}))))

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

(defn lane [tiles creeps]
  {:creeps creeps :tiles tiles})

(defn hero-with-name [heroes name]
  (first (set/select #(= (:name %) name) heroes)))

(defn creep-with-name [creeps name]
  (first (set/select #(= (:name %) name) creeps)))

(defn tiles-from-to [from to]
  (map (fn [x y _] {:x x
                    :y y
                    :width (get-in configuration [:tiles :width])
                    :height (get-in configuration [:tiles :height])})
       (lazy-cat (range (:x from)
                        (+ (:x to) (get-in configuration [:tiles :width]))
                        ((case (> (:x to) (:x from)) true + false - :else +)
                         (get-in configuration [:tiles :width])))
                 (repeat (:x to)))
       (lazy-cat (range (:y from)
                        (+ (:y to) (get-in configuration [:tiles :height]))
                        ((case (> (:y to) (:y from)) true + false - :else +)
                         (get-in configuration [:tiles :height])))
                 (repeat (:y to)))
       (range (inc (max (/ (Math/abs (- (:x to) (:x from)))
                           (get-in configuration [:tiles :width]))
                        (/ (Math/abs (- (:y to) (:y from)))
                           (get-in configuration [:tiles :height])))))))

(def canvas
  (ui/canvas {:container "canvas-container"
              :width 800
              :height 800
              :listening false}))

;; FIXME actually implement a map sorted by z-index.
(def layers
  (sorted-map :tiles (ui/layer {:listening false})
              :paths (ui/layer {:listening false})
              :bases (ui/layer {:listening false})
              :creeps (ui/layer {:listening false})
              :heroes (ui/layer {:listening false})
              :value-bars (ui/layer {:listening false})))

(defn tiles-to-the-top [thing n]
  (merge thing {:y (- (:y thing) (* n (get-in configuration [:tiles :height])))}))

(defn tiles-to-the-bottom [thing n]
  (merge thing {:y (+ (:y thing) (* n (get-in configuration [:tiles :height])))}))

(defn tiles-to-the-right [thing n]
  (merge thing {:x (+ (:x thing) (* n (get-in configuration [:tiles :width])))}))

(defn tiles-to-the-left [thing n]
  (merge thing {:x (- (:x thing) (* n (get-in configuration [:tiles :width])))}))

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
        dire-top-lane-creeps [(merge melee-creep {:id (uuid)
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
                                                                tile-height)))})]
        dire-top-lane-tiles-origin (tiles-to-the-bottom (tiles-to-the-left dire-base
                                                                           2)
                                                        2)
        dire-top-lane-tiles-destination (tiles-to-the-top (tiles-to-the-right radiant-base
                                                                              2)
                                                          2)
        dire-top-lane-tiles-middle {:x (:x dire-top-lane-tiles-destination)
                                    :y (:y dire-top-lane-tiles-origin)
                                    :width (get-in configuration [:tiles :width])
                                    :height (get-in configuration [:tiles :height])}
        dire-top-lane-tiles (distinct (concat (tiles-from-to dire-top-lane-tiles-origin
                                                             dire-top-lane-tiles-middle)
                                              (tiles-from-to dire-top-lane-tiles-middle
                                                             dire-top-lane-tiles-destination)))
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

(let [tiles (get-in @state [:map :tiles])
      bases (map :base (vals (:teams @state)))
      heroes (flatten (map vals (map :heroes (vals (:teams @state)))))
      teams (:teams @state)
      dire-creeps (get-in (:dire teams) [:lanes :top :creeps])
      radiant-creeps (get-in (:radiant teams) [:lanes :top :creeps])
      creeps (concat dire-creeps radiant-creeps)]
  (ui/add-views (:tiles layers)
                (ui/tile-views (map (fn [index]
                                      {:path (conj [:map :tiles] index) :state state})
                                    (range (count tiles)))))
  (ui/add-views (:bases layers) (ui/base-views (map (partial to-cursor state) bases)))
  (ui/add-views (:heroes layers) (ui/hero-views (map (partial to-cursor state) heroes)))
  (ui/add-views (:creeps layers) (ui/creep-views (map (partial to-cursor state) creeps)))
  (ui/add-layers canvas (reverse (vals layers))))

(state/move-towards! state (get-in @state [:teams :radiant :heroes :lion]) {:x 500 :y 50})
(state/move-towards! state (get-in @state [:teams :dire :heroes :anti-mage]) {:x 50 :y 500})
;; (state/move-towards! state (get-in @state [:teams :dire :lanes :top :creeps 0]) {:x 60 :y 60})
;; (state/move-towards! state (get-in @state [:teams :dire :lanes :top :creeps 1]) {:x 60 :y 60})
;; (state/move-towards! state (get-in @state [:teams :dire :lanes :top :creeps 2]) {:x 60 :y 60})

(log
  (let [creep (get-in @state [:teams :dire :lanes :top :creeps 0])]
    {:coordinates (battle-arena.state/creep-coordinates creep)
     :destination (select-keys (:destination creep) [:x :y])}))
(log
  (let [lane (get-in @state [:teams :dire :lanes :top])]
    (last (:tiles lane))))

(log
  (let [lane (get-in @state [:teams :dire :lanes :top])]
    (map #(select-keys % [:x :y :destination]) (:creeps lane))))

(log
  (let [lane (get-in @state [:teams :dire :lanes :top])]
    (map #(select-keys % [:x :y :destination])
         (battle-arena.state/next-creeps-state
           (battle-arena.state/update-lane-creeps-destination lane)))))

(log
  (let [lane (get-in @state [:teams :dire :lanes :top])]
    (map #(select-keys % [:x :y :destination])
         (battle-arena.state/update-lane-creeps-destination lane))))

(log
  (let [lane (get-in @state [:teams :dire :lanes :top])
        creep (get-in @state [:teams :dire :lanes :top :creeps 0])]
    (battle-arena.state/creep-within-lane? creep lane)))

(log
  (let [lane (get-in @state [:teams :dire :lanes :top])
        creep (get-in @state [:teams :dire :lanes :top :creeps 0])]
    [{:coordinates (battle-arena.state/creep-coordinates creep)
     :destination (select-keys (:destination creep) [:x :y])}
     (battle-arena.state/next-lane-tile lane creep)]))

(log
  (let [lane (get-in @state [:teams :dire :lanes :top])
        creep (get-in @state [:teams :dire :lanes :top :creeps 0])]
    (battle-arena.state/next-lane-creep-destination lane creep)))

(log
  (let [lane (get-in @state [:teams :dire :lanes :top])
        creep (get-in @state [:teams :dire :lanes :top :creeps 0])]
    (battle-arena.state/lane-tile-below-creep lane creep)))

(log
  (let [lane (get-in @state [:teams :dire :lanes :top])
        creep (get-in @state [:teams :dire :lanes :top :creeps 0])]
    (log [creep (nth (:tiles lane) 2)])
    (some #(battle-arena.state/creep-within-tile? creep %) (:tiles lane))))

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
                   (fn [e] (.preventDefault e) (dorun (repeatedly (nth t 1) tick)))))

(def fpsmeter (js/FPSMeter.))

(go
  (loop [previous-state @state current-state @state]
    ;; (<! tick-chan)
    (<! (timeout (:state-change-interval configuration)))
    (ui/update-canvas! canvas previous-state current-state)
    (ui/draw-canvas! canvas)
    (.tick fpsmeter)
    (recur current-state (next-state! state))))
