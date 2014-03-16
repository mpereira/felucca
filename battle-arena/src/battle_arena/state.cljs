(ns battle-arena.state
  (:require [clojure.zip :as zip]
            [clojure.set :as set]
            [battle-arena.geometry :refer [point-in-triangle?
                                           point-distance]]))

(defn round [n decimals]
  (/ (Math.round (* n (Math.pow 10 decimals))) (Math.pow 10 decimals)))

(defn heterogeneous-zipper [m]
  (zip/zipper #(constantly true)
              #(seq (cond (map? %) (vals %) (vector? %) % :else nil))
              #(constantly %)
              m))

(defn index-of
  ([s value] (index-of s value 0))
  ([s value index]
   (when-not (empty? s)
     (if (= value (first s)) index (recur (rest s) value (inc index))))))

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

(defn movement-speed [hero]
  1)

(defn creep-coordinates [creep]
  (select-keys creep [:x :y]))

(defn tile-coordinates [tile]
  (select-keys tile [:x :y]))

(defn coordinates-within-tile? [coordinates tile]
  (and
    (<= (:x tile) (:x coordinates) (+ -1 (:x tile) (:width tile)))
    (<= (:y tile) (:y coordinates) (+ -1 (:y tile) (:height tile)))))

(defn lane-tile-at [lane coordinates]
  (first
    (filter (partial coordinates-within-tile? coordinates) (:tiles lane))))

(defn tile-distance [a b]
  (apply point-distance (map (juxt :x :y) [a b])))

(defn distance [a b]
  (apply point-distance (map (juxt :x :y) [a b])))

(defn same-coordinates? [a b]
  (= (creep-coordinates a) (creep-coordinates b)))

(defn creep-within-tile? [creep tile]
  (coordinates-within-tile? (creep-coordinates creep) tile))

(defn creep-inside-two-subsequent-tiles-path?
  [{cx :x cy :y :as creep} [{t1x :x t1y :y} {t2x :x t2y :y} :as tiles]]
  (or (some (partial creep-within-tile? creep) tiles)
      (if (= (- t1x t2x) (- t1y t2y)) ;; -45 degrees
        (or (point-in-triangle? [cx cy]
                                [[(max t1x t2x) (min t1y t2y)]
                                 [(max t1x t2x) (max t1y t2y)]
                                 [(+ (max t1x t2x) (Math/abs (- t1x t2x)))
                                  (max t1y t2y)]])
            (point-in-triangle? [cx cy]
                                [[(min t1x t2x) (max t1y t2y)]
                                 [(max t1x t2x) (max t1y t2y)]
                                 [(max t1x t2x)
                                  (+ (max t1y t2y) (Math/abs (- t1y t2y)))]]))
        (or (point-in-triangle? [cx cy]
                                [[(min t1x t2x) (max t1y t2y)]
                                 [(max t1x t2x) (max t1y t2y)]
                                 [(max t1x t2x) (min t1y t2y)]])
            (point-in-triangle? [cx cy]
                                [[(max t1x t2x)
                                  (+ (max t1y t2y) (Math/abs (- t1y t2y)))]
                                 [(max t1x t2x) (max t1y t2y)]
                                 [(+ (max t1x t2x) (Math/abs (- t1x t2x)))
                                  (max t1y t2y)]])))))

(defn creep-within-lane? [creep {:keys [tiles]}]
  (some #(creep-inside-two-subsequent-tiles-path? creep %)
        (partition 2 1 tiles)))

(defn lane-tile-below-creep [{:keys [tiles]} creep]
  (first (filter (partial creep-within-tile? creep) tiles)))

(defn tile-closest-to-creep [tiles creep]
  (first (sort-by (partial distance creep) tiles)))

(defn next-lane-tile [{:keys [tiles]} creep]
  (let [t (tile-closest-to-creep tiles creep)]
    (if (same-coordinates? t (last tiles))
      (last tiles)
      (second (drop-while (partial not= t) tiles)))))

(defn closest-lane-tile [{:keys [tiles]} tile]
  (first (sort-by (partial distance tile) tiles)))

(defn next-lane-creep-destination [lane creep]
  (if (creep-within-lane? creep lane)
    (next-lane-tile lane creep)
    (closest-lane-tile lane creep)))

(defn update-lane-creeps-destination [{:keys [creeps tiles] :as lane}]
  (map #(assoc % :destination (next-lane-creep-destination lane %)) creeps))

(defn move-towards! [state hero tile]
  (swap! state
         (fn [state hero tile]
           (update-in state
                      (path state hero)
                      merge
                      {:destination (select-keys tile [:x :y])}))
         hero
         tile))

;; TODO smaller delta if moving diagonally.
(defn next-coordinates-delta [creature]
  (let [destination (:destination creature)
        delta-x (- (:x destination) (:x creature))
        delta-y (- (:y destination) (:y creature))]
    {:x (cond (pos? delta-x) (movement-speed creature)
              (neg? delta-x) (- (movement-speed creature))
              :else 0)
     :y (cond (pos? delta-y) (movement-speed creature)
              (neg? delta-y) (- (movement-speed creature))
              :else 0)}))

(defn next-hero-state [hero]
  (if-let [destination (:destination hero)]
    (if (= (tile-coordinates destination) (select-keys hero [:x :y]))
      (dissoc hero :destination)
      (merge-with + hero (next-coordinates-delta hero)))
    hero))

(defn next-creep-state [creep]
  (if-let [destination (:destination creep)]
    (if (= (tile-coordinates destination) (creep-coordinates creep))
      (dissoc creep :destination)
      (merge-with + creep (next-coordinates-delta creep)))
    creep))

(defn next-creeps-state [creeps]
  (into [] (map next-creep-state creeps)))

(defn next-lane-state [{:keys [tiles creeps] :as lane}]
  {:tiles tiles
   :creeps (next-creeps-state (update-lane-creeps-destination lane))})

(defn next-team-state [team])
(defn next-teams-state [teams] (map next-team-state teams))

(defn next-state [state]
  (-> state
      (update-in [:teams :dire :lanes :top] next-lane-state)
      (update-in [:teams :radiant :heroes :lion] next-hero-state)
      (update-in [:teams :dire :heroes :anti-mage] next-hero-state)))

(defn next-state! [state]
  (swap! state next-state))
