(ns src.battle-arena.components.rts-camera
  (:use arcadia.core)
  (:import [UnityEngine
            Debug
            Time
            Transform
            Vector3
            Input
            Space
            Rect]))

(defn update! [this]
  (let [border (.. this border)
        speed  (.. this speed)
        bottom (Rect. 0 0 Screen/width border)
        top    (Rect. 0 (- Screen/height border) Screen/width border)
        left   (Rect. 0 0 border Screen/height)
        right  (Rect. (- Screen/width border) 0 border Screen/height)]
    (when (.Contains bottom Input/mousePosition)
      (.Translate (.. this transform)
                  (* speed Time/deltaTime)
                  0
                  (- (* speed Time/deltaTime))
                  Space/World))
    (when (.Contains top Input/mousePosition)
      (.Translate (.. this transform)
                  (- (* speed Time/deltaTime))
                  0
                  (* speed Time/deltaTime)
                  Space/World))
    (when (.Contains left Input/mousePosition)
      (.Translate (.. this transform)
                  (- (* speed Time/deltaTime))
                  0
                  (- (* speed Time/deltaTime))
                  Space/World))
    (when (.Contains right Input/mousePosition)
      (.Translate (.. this transform)
                  (* speed Time/deltaTime)
                  0
                  (* speed Time/deltaTime)
                  Space/World))))

(defcomponent RTSCamera [^float speed ^float border]
  (Update [this] (update! this)))

;; FIXME: trying to `import` types provided by `defcomponent` result in:
;;
;; System.NullReferenceException: Object reference not set to an instance of an
;; object
(def Component RTSCamera)
