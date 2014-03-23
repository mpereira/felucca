(ns battle-arena.ui)

;; TODO: make group/layer constructors automatically compute their coordinates
;; and dimensions based on their children.

(defn find-view [view selector]
  (.find view selector))

(defn find-view-by-id [view id]
  (find-view view (str "#" id)))

(defn find-view-by-name [view name]
  (find-view view (str "." name)))

(defn tile-view [{:keys [path state]}]
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
    group))

(defn tile-views [cursors] (map tile-view cursors))

(defn base-view [{:keys [path state]}]
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
        {:keys [id coordinates dimensions fill stroke stroke-width]} value
        group (Kinetic.Group. #js {:id id
                                   :x (:x coordinates)
                                   :y (:y coordinates)
                                   :listening false})
        rectangle (Kinetic.Rect. #js {:name "hero-rectangle"
                                      :width (:width dimensions)
                                      :height (:height dimensions)
                                      :fill fill
                                      :stroke stroke
                                      :strokeWidth stroke-width
                                      :listening false})]
    (.add group rectangle)
    group))

(defn hero-views [cursors] (map hero-view cursors))

(defn update-hero-view! [canvas hero]
  (let [{:keys [coordinates dimensions fill stroke stroke-width]} hero
        view (first (find-view-by-id canvas (:id hero)))
        hero-rectangle (first (find-view-by-name view "hero-rectangle"))]
    (.setAttrs view #js {:x (:x coordinates) :y (:y coordinates)})
    (.setAttrs hero-rectangle #js {:width (:width dimensions)
                                   :height (:height dimensions)
                                   :fill fill
                                   :stroke stroke
                                   :strokeWidth stroke-width})
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

(defn value-bar-view [] (Kinetic.Group. #js {:listening false}))
(defn value-bar-views [] (Kinetic.Group. #js {:listening false}))

(defn update-canvas! [canvas previous-state current-state]
  (update-hero-view! canvas (get-in current-state [:teams :radiant :heroes :lion]))
  (update-hero-view! canvas (get-in current-state [:teams :dire :heroes :anti-mage]))
  (update-creep-views! canvas (get-in current-state [:teams :dire :lanes :top :creeps])))

(defn draw-canvas! [canvas]
  (doseq [layer (.getLayers canvas)] (.draw layer)))

(defn add-layer [canvas layer]
  (.add canvas layer))

(defn add-layers [canvas layers]
  (doseq [layer layers] (add-layer canvas layer)))

(defn add-view [layer view]
  (.add layer view))

(defn add-views [layer views]
  (doseq [view views] (add-view layer view)))

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

(defn cache [shape]
  (.cache shape (cljs->js {:x (.x shape)
                           :y (.y shape)
                           :width (.width shape)
                           :height (.height shape)})))

(defn canvas [options]
  (Kinetic.Stage. (cljs->js options)))

(defn layer [options]
  (Kinetic.Layer. (cljs->js options)))
