(ns src.battle-arena.components.click-to-move
  (:use arcadia.core
        src.battle-arena.vector3)
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

(defn- set-target-position! [this next-target-position]
  (set! (.. this target-position) (v3 (.x next-target-position)
                                      (.. this transform localPosition y)
                                      (.z next-target-position))))

(defn- look-towards-target-position! [this]
  (let [current-position (.. this transform localPosition)
        target-position  (.. this target-position)]
    (if (> (vdistance current-position target-position) 1)
      (let [current-rotation (.. this transform localRotation)
            target-rotation  (Quaternion/LookRotation (v* (v3 1 0 1)
                                                          (v- target-position
                                                              current-position)))]
        (set! (.. this transform localRotation)
              (Quaternion/RotateTowards current-rotation
                                        target-rotation
                                        (* (.. this rotation-speed)
                                           Time/deltaTime)))))))

(defn- move-towards-target-position! [this]
  (let [current-position (.. this transform localPosition)
        target-position  (.. this target-position)
        controller       (.. this (GetComponent CharacterController))]
    (if (> (vdistance current-position target-position) 1)
      (let [next-position (v* (.forward (.. this transform))
                              (.. this movement-speed))
            next-position-normalized (v3 (.x next-position)
                                         0
                                         (.z next-position))]
        (.. controller (SimpleMove next-position-normalized))))))

(defn start! [this]
  (set-target-position! this (.. this transform localPosition)))

(defn update! [this]
  (when (Input/GetKeyDown KeyCode/Mouse0)
    (let [ray (.ScreenPointToRay (.. this camera) Input/mousePosition)
          hits (Physics/RaycastAll ray Mathf/Infinity Physics/DefaultRaycastLayers)
          target-hit (get hits (- (count hits) 1))]
      (set-target-position! this (.point target-hit))))
  (look-towards-target-position! this)
  (move-towards-target-position! this))

(defcomponent ClickToMove [^Camera  camera
                           ^float   movement-speed
                           ^float   rotation-speed
                           ^Vector3 target-position]
  (Start  [this] (start!  this))
  (Update [this] (update! this)))
