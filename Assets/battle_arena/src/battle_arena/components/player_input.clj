(ns battle-arena.components.player-input
  (:use arcadia.core)
  (:require [battle-arena.vector3 :as v3]
            [battle-arena.utils :as utils]
            [battle-arena.components.creature :as creature])
  (:import [UnityEngine
            Application
            Camera
            CharacterController
            Debug
            KeyCode
            Mathf
            Physics
            Quaternion
            Ray
            RaycastHit
            Time
            Vector3]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmutable PlayerInput [])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Domain logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn target-hit []
  (let [ray (.ScreenPointToRay Camera/main Input/mousePosition)
        hits (utils/->seq (Physics/RaycastAll ray
                                              Mathf/Infinity
                                              Physics/DefaultRaycastLayers))]
    (last hits)))

(defmulti terrain? class)

(defmethod terrain? RaycastHit [raycast-hit]
  (= "Terrain" (.. raycast-hit transform gameObject name)))

(defmulti creature? class)

(defmethod creature? RaycastHit [raycast-hit]
  (not (nil? (lookup (.. raycast-hit transform gameObject) :creature))))

(defn handle-terrain-click! [^GameObject this raycast-hit]
  (log "terrain click!")
  (creature/stop-attacking! this)
  (creature/set-destination! this (let [point (.point raycast-hit)]
                                    (v3/v3 (.x point)
                                           (.. this transform localPosition y)
                                           (.z point)))))

(defn handle-creature-click! [this raycast-hit]
  (log "creature click!")
  (let [creature (.. raycast-hit transform gameObject)]
    (log "creature click" (.name creature))
    (creature/start-attacking! this creature)
    (creature/acknowledge-attacker! creature this)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hooks ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update! [^GameObject this role-key]
  (when (Input/GetKeyDown KeyCode/Mouse1)
    (when-let [raycast-hit (target-hit)]
      (cond (terrain? raycast-hit) (handle-terrain-click! this raycast-hit)
            (creature? raycast-hit) (handle-creature-click! this raycast-hit)))))

(def hooks
  {:update #'update!})
