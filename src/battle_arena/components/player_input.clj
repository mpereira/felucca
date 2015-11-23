(ns src.battle-arena.components.player-input
  (:use arcadia.core)
  (:require [src.battle-arena.vector3 :refer :all]
            [src.battle-arena.utils :refer :all]
            [src.battle-arena.components.hero :as hero])
  (:import [UnityEngine
            Application
            Camera
            CharacterController
            Debug
            KeyCode
            Mathf
            Physics
            Ray
            RaycastHit
            Time]))

(defn moving-towards-target? [this] (.moving-towards-target? this))
(defn target-position [this] (.target-position this))
(defn attackee [this] (.attackee this))
(defn attacking-attackee? [this] (.attacking-attackee? this))
(defn last-hit-attempted-at [this] (.last-hit-attempted-at this))

(defn recovered-from-previous-hit? [this]
  (or (not (last-hit-attempted-at this))
      (> (- Time/time (last-hit-attempted-at this))
         (hero/attack-speed (hero/component this)))))

(defn within-attack-range? [this creature]
  (> (hero/attack-range (hero/component this))
     (vdistance (.. this transform localPosition)
                (.. creature transform localPosition))))

(defn hit! [this creature]
  (set! (.last-hit-attempted-at this) Time/time)
  (hero/receive-hit! (hero/component creature) 5))

(defn attempt-hit! [this creature]
  (when (and (hero/alive? (hero/component creature))
             (recovered-from-previous-hit? this))
    (hit! this creature)))

(defn set-target-position! [this position]
  (set! (.target-position this)
        (v3 (.x position) (.. this transform localPosition y) (.z position))))

(defn look-towards-position! [this position]
  (let [current-position (.. this transform localPosition)]
    (if (> (vdistance current-position position) 1)
      (let [current-rotation (.. this transform localRotation)
            target-rotation  (Quaternion/LookRotation (v* (v3 1 0 1)
                                                          (v- position
                                                              current-position)))]
        (set! (.. this transform localRotation)
              (Quaternion/RotateTowards current-rotation
                                        target-rotation
                                        (* (hero/rotation-speed
                                            (hero/component this))
                                           Time/deltaTime)))))))

(defn move-towards-position! [this position]
  (let [current-position (.. this transform localPosition)
        controller       (.. this (GetComponent CharacterController))]
    (if (> (vdistance current-position position) 1)
      (.SimpleMove controller (v* (.. this transform forward)
                                  (hero/movement-speed
                                   (hero/component this)))))))

(defn handle-terrain-click! [this raycast-hit]
  (set! (.attacking-attackee? this) false)
  (set! (.moving-towards-target? this) true)
  (set-target-position! this (.point raycast-hit)))

(defn handle-enemy-click! [this raycast-hit]
  (set! (.attackee this) (.. raycast-hit transform gameObject))
  (set! (.attacking-attackee? this) true)
  (set! (.. this moving-towards-target?) false))

(defn update! [this]
  (when (Input/GetKeyDown KeyCode/Mouse1)
    (when-let [raycast-hit (target-hit)]
      (cond (terrain? raycast-hit) (handle-terrain-click! this raycast-hit)
            (enemy-hero? raycast-hit) (handle-enemy-click! this raycast-hit))))
  (when (.moving-towards-target? this)
    (look-towards-position! this (.target-position this))
    (move-towards-position! this (.target-position this)))
  (when (.attacking-attackee? this)
    (let [attackee-position (.. this attackee transform localPosition)]
      (look-towards-position! this attackee-position)
      (move-towards-position! this attackee-position)
      (when (within-attack-range? this (attackee this))
        (attempt-hit! this (attackee this))))))

(defcomponent PlayerInput [^bool moving-towards-target?
                           ^Vector3 target-position
                           ^GameObject attackee
                           ^bool attacking-attackee?
                           ^float last-hit-attempted-at]
  (Update [this] (update! this)))

;; FIXME: trying to `import` types provided by `defcomponent` result in:
;;
;; System.NullReferenceException: Object reference not set to an instance of an
;; object
(def Component PlayerInput)
(defn component [that] (.GetComponent that Component))
