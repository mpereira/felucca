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
  {:creeps creeps :tiles tiles})

(defn hero-with-name [heroes name]
  (first (set/select #(= (:name %) name) heroes)))

(defn creep-with-name [creeps name]
  (first (set/select #(= (:name %) name) creeps)))

(defn tiles-from-to [{from-coordinates :coordinates} {to-coordinates :coordinates}]
  (map (fn [x y _] {:coordinates {:x x :y y}
                    :dimensions {:width (get-in configuration [:tiles :dimensions :width])
                                 :height (get-in configuration [:tiles :dimensions :height])}})
       (lazy-cat (range (:x from-coordinates)
                        (+ (:x to-coordinates) (get-in configuration [:tiles :dimensions :width]))
                        ((case (> (:x to-coordinates) (:x from-coordinates)) true + false - :else +)
                         (get-in configuration [:tiles :dimensions :width])))
                 (repeat (:x to-coordinates)))
       (lazy-cat (range (:y from-coordinates)
                        (+ (:y to-coordinates) (get-in configuration [:tiles :dimensions :height]))
                        ((case (> (:y to-coordinates) (:y from-coordinates)) true + false - :else +)
                         (get-in configuration [:tiles :dimensions :height])))
                 (repeat (:y to-coordinates)))
       (range (inc (max (/ (Math/abs (- (:x to-coordinates) (:x from-coordinates)))
                           (get-in configuration [:tiles :dimensions :width]))
                        (/ (Math/abs (- (:y to-coordinates) (:y from-coordinates)))
                           (get-in configuration [:tiles :dimensions :height])))))))

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
           :map {:tiles map-tiles}
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

(state/move-towards! state
                     (get-in @state [:teams :radiant :heroes :lion])
                     {:coordinates {:x 500 :y 50}})
(state/move-towards! state
                     (get-in @state [:teams :dire :heroes :anti-mage])
                     {:coordinates {:x 50 :y 500}})

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
                   #(do (.preventDefault %) (dorun (repeatedly (nth t 1) tick)))))

(def fpsmeter (js/FPSMeter.))

(go
  (loop [previous-state @state current-state @state]
    ;; (<! tick-chan)
    (<! (timeout (:state-change-interval configuration)))
    (ui/update-canvas! canvas previous-state current-state)
    (ui/draw-canvas! canvas)
    (.tick fpsmeter)
    (recur current-state (next-state! state))))
