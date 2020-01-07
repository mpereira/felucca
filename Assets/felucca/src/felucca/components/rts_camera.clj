(ns felucca.components.rts-camera
  (:use arcadia.core)
  (:import [UnityEngine
            Debug
            Time
            Transform
            Vector3
            Input
            Space
            Rect]))

(defn update! [game-object role-key]
  (let [rts-camera (state game-object :rts-camera)]
    (let [border (:border rts-camera)
          speed  (:speed rts-camera)
          bottom (Rect. 0 0 Screen/width border)
          top    (Rect. 0 (- Screen/height border) Screen/width border)
          left   (Rect. 0 0 border Screen/height)
          right  (Rect. (- Screen/width border) 0 border Screen/height)]
      (when (.Contains bottom Input/mousePosition)
        (.Translate (.. game-object transform)
                    (* speed Time/deltaTime)
                    0
                    (- (* speed Time/deltaTime))
                    Space/World))
      (when (.Contains top Input/mousePosition)
        (.Translate (.. game-object transform)
                    (- (* speed Time/deltaTime))
                    0
                    (* speed Time/deltaTime)
                    Space/World))
      (when (.Contains left Input/mousePosition)
        (.Translate (.. game-object transform)
                    (- (* speed Time/deltaTime))
                    0
                    (- (* speed Time/deltaTime))
                    Space/World))
      (when (.Contains right Input/mousePosition)
        (.Translate (.. game-object transform)
                    (* speed Time/deltaTime)
                    0
                    (* speed Time/deltaTime)
                    Space/World)))))

(defmutable RTSCamera [^float speed ^float border])

(def hooks
  {:update #'update!})
