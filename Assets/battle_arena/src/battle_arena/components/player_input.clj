(ns battle-arena.components.player-input
  (:use arcadia.core)
  (:require [battle-arena.vector3 :refer :all]
            [battle-arena.utils :refer :all]
            [battle-arena.components.hero :as hero])
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

(defn moving-towards-target? [game-object]
  (let [player-input (state game-object :player-input)]
    (:moving-towards-target? player-input)))

(defn target-position [game-object]
  (let [player-input (state game-object :player-input)]
    (:target-position player-input)))

(defn attackee [game-object]
  (let [player-input (state game-object :player-input)]
    (:attackee player-input)))

(defn attacking-attackee? [game-object]
  (let [player-input (state game-object :player-input)]
    (:attacking-attackee? player-input)))

(defn last-hit-attempted-at [game-object]
  (let [player-input (state game-object :player-input)]
    (:last-hit-attempted-at player-input)))

(defn recovered-from-previous-hit? [game-object]
  (or (not (last-hit-attempted-at game-object))
      (> (- Time/time (last-hit-attempted-at game-object))
         (hero/attack-speed game-object))))

(defn within-attack-range? [game-object attackee]
  (> (hero/attack-range game-object)
     (vdistance (.. game-object transform localPosition)
                (.. attackee transform localPosition))))

(defn hit! [game-object attackee]
  (let [hero (state game-object :hero)]
    (update-state game-object
                  :player-input
                  (fn [player-input]
                    (assoc player-input
                           :last-hit-attempted-at Time/time)))
    (hero/receive-hit! attackee 5)))

(defn attempt-hit! [game-object attackee]
  (when (and (hero/alive? attackee)
             (recovered-from-previous-hit? game-object))
    (hit! game-object attackee)))

(defn look-towards-position! [game-object position]
  (let [current-position (.. game-object transform localPosition)]
    (if (> (vdistance current-position position) 1)
      (let [current-rotation (.. game-object transform localRotation)
            target-rotation  (Quaternion/LookRotation (v* (v3 1 0 1)
                                                          (v- position
                                                              current-position)))]
        (set! (.. game-object transform localRotation)
              (Quaternion/RotateTowards current-rotation
                                        target-rotation
                                        (* (hero/rotation-speed game-object)
                                           Time/deltaTime)))))))

(defn move-towards-position! [game-object position]
  (let [current-position (.. game-object transform localPosition)
        controller       (.. game-object (GetComponent CharacterController))]
    (if (> (vdistance current-position position) 1)
      (.SimpleMove controller (v* (.. game-object transform forward)
                                  (hero/movement-speed game-object))))))

(defn handle-terrain-click! [game-object raycast-hit]
  (update-state
   game-object
   :player-input
   (fn [player-input]
     (assoc player-input
            :attacking-attackee? false
            :moving-towards-target? true
            :target-position (let [position (.point raycast-hit)]
                               (v3 (.x position)
                                   (.. game-object transform localPosition y)
                                   (.z position)))))))

(defn handle-enemy-click! [game-object raycast-hit]
  (update-state
   game-object
   :player-input
   (fn [player-input]
     (assoc player-input
            :attacking-attackee? true
            :attackee (.. raycast-hit transform gameObject)
            :moving-towards-target? false))))

(defn update! [game-object role-key]
  (let [player-input (state game-object :player-input)]
    (when (Input/GetKeyDown KeyCode/Mouse1)
      (when-let [raycast-hit (target-hit)]
        (cond (terrain? raycast-hit)
              (handle-terrain-click! game-object raycast-hit)
              (enemy-hero? raycast-hit)
              (handle-enemy-click! game-object raycast-hit))))
    (when (:moving-towards-target? player-input)
      (look-towards-position! game-object (:target-position player-input))
      (move-towards-position! game-object (:target-position player-input)))
    (when (:attacking-attackee? player-input)
      (let [attackee-position (.. (:attackee player-input) transform localPosition)]
        (look-towards-position! game-object attackee-position)
        (move-towards-position! game-object attackee-position)
        (when (within-attack-range? game-object (attackee game-object))
          (attempt-hit! game-object (attackee game-object)))))))

(defmutable PlayerInput [^bool moving-towards-target?
                         ^Vector3 target-position
                         ^GameObject attackee
                         ^bool attacking-attackee?
                         ^float last-hit-attempted-at])

(def hooks
  {:update #'update!})
