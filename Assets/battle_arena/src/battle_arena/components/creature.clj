(ns battle-arena.components.creature
  (:require [arcadia.core :refer [defmutable log update-state state log]]
            [battle-arena.vector3 :as v3])
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

(defmutable Creature [^String name

                      ^int strength
                      ^int dexterity
                      ^int intelligence

                      ^int hit-points
                      ^int stamina
                      ^int mana

                      ^int movement-speed
                      ^int rotation-speed
                      ^int attack-speed
                      ^int attack-range

                      ^Vector3 position
                      ^Vector3 destination

                      ^GameObject attackee
                      ^float last-hit-attempted-at
                      #^"UnityEngine.GameObject[]" threateners])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Getters ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn strength [^GameObject this] (:strength (state this :creature)))
(defn dexterity [^GameObject this] (:dexterity (state this :creature)))
(defn intelligence [^GameObject this] (:intelligence (state this :creature)))

(defn hit-points [^GameObject this] (:hit-points (state this :creature)))
(defn stamina [^GameObject this] (:stamina (state this :creature)))
(defn mana [^GameObject this] (:mana (state this :creature)))

(defn movement-speed [^GameObject this] (:movement-speed (state this :creature)))
(defn rotation-speed [^GameObject this] (:rotation-speed (state this :creature)))
(defn attack-speed [^GameObject this] (:attack-speed (state this :creature)))
(defn attack-range [^GameObject this] (:attack-range (state this :creature)))

(defn position [^GameObject this] (:position (state this :creature)))
(defn destination [^GameObject this] (:destination (state this :creature)))

(defn attackee [^GameObject this] (:attackee (state this :creature)))
(defn last-hit-attempted-at [^GameObject this] (:last-hit-attempted-at (state this :creature)))
(defn threateners [^GameObject this] (:threateners (state this :creature)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Domain logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn normalized-attack-range [^GameObject this]
  (/ (attack-range this)
     50))

(defn normalized-attack-speed [^GameObject this]
  (/ (attack-speed this)
     10))

(defn normalized-movement-speed [^GameObject this]
  (/ (movement-speed this)
     10))

(defn normalized-rotation-speed [^GameObject this]
  (* 10
     (rotation-speed this)))

(defn max-hit-points [^GameObject this] (strength this))

(defn hit-points-percentage [^GameObject this]
  (/ (hit-points this) (max-hit-points this)))

(defn closest-threatener [^GameObject this]
  (first (threateners this)))

(defn dead? [^GameObject this] (>= 0 (hit-points this)))
(def alive? (complement dead?))

(defn attacking-something? [^GameObject this] (not (nil? (attackee this))))

(defn moving-towards-something? [^GameObject this]
  (not (v3/vempty? (destination this))))

(defn recovered-from-previous-hit? [^GameObject this]
  (or (not (last-hit-attempted-at this))
      (> (- Time/time (last-hit-attempted-at this))
         (normalized-attack-speed this))))

(defn within-attack-range? [^GameObject this
                            ^GameObject attackee]
  (> (normalized-attack-range this)
     (v3/vdistance (.. this transform localPosition)
                   (.. attackee transform localPosition))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn stop-attacking! [^GameObject this]
  (update-state this :creature (fn [creature-state]
                                 (assoc creature-state :attackee nil))))

(defn set-destination! [^GameObject this
                        ^Vector3 destination]
  (update-state this
                :creature
                (fn [creature-state]
                  (assoc creature-state :destination destination))))

(defn start-attacking! [^GameObject this
                        ^GameObject attackee]
  (update-state this
                :creature
                (fn [creature-state]
                  (assoc creature-state :attackee attackee))))

(defn acknowledge-attacker! [^GameObject this
                             ^GameObject attacker]
  (update-state this
                :creature
                (fn [creature-state]
                  (-> creature-state
                      (assoc :attackee attacker)
                      (update :threateners #(conj % attacker))))))

(defn set-hit-points! [^GameObject this value]
  (log "set hit points!" value)
  (update-state this :creature (fn [creature-state]
                                 (assoc creature-state :hit-points value))))

(defn decrement-hit-points! [^GameObject this value]
  (set-hit-points! this (max 0 (- (hit-points this) value))))

(defn increment-hit-points! [^GameObject this value]
  (set-hit-points! this (min (max-hit-points this) (+ (hit-points this) value))))

(defn receive-hit! [^GameObject this hit]
  (decrement-hit-points! this hit))

(defn hit! [^GameObject this
            ^GameObject attackee]
  (update-state this
                :creature
                (fn [creature-state]
                  (assoc creature-state :last-hit-attempted-at Time/time)))
  ;; TODO: damage, hit/miss, critical.
  (receive-hit! attackee 5))

(defn attempt-hit! [^GameObject this
                    ^GameObject attackee]
  (when (recovered-from-previous-hit? this)
    (hit! this attackee)))

(defn look-towards-position! [^GameObject this
                              ^Vector3 position]
  (let [current-position (.. this transform localPosition)]
    (let [current-rotation (.. this transform localRotation)
          target-rotation  (Quaternion/LookRotation
                            (v3/v* (v3/v3 1 0 1)
                                   (v3/v- position current-position)))]
      (set! (.. this transform localRotation)
            (Quaternion/RotateTowards current-rotation
                                      target-rotation
                                      (* (normalized-rotation-speed this)
                                         Time/deltaTime))))))

(defn move-towards-position! [^GameObject this
                              ^Vector3 position]
  (let [current-position (.. this transform localPosition)
        controller (.. this (GetComponent CharacterController))]
    (if (> (v3/vdistance current-position position) 1)
      (.SimpleMove controller (v3/v* (.. this transform forward)
                                     (normalized-movement-speed this))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hooks ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start! [^GameObject this role-key]
  (log "creature start!" (.name this))
  (update-state this
                :creature
                (fn [{:keys [strength dexterity intelligence]
                      :as creature-state}]
                  (assoc creature-state
                         :hit-points strength
                         :stamina dexterity
                         :mana intelligence))))

(defn update! [^GameObject this role-key]
  (when (alive? this)
    (when-let [attackee* (attackee this)]
      (when (alive? attackee*)
        (look-towards-position! this
                                (.. attackee* transform localPosition))
        (move-towards-position! this (.. attackee* transform localPosition))
        (let [attackee*-position (.. attackee* transform localPosition)]
          (when (within-attack-range? this attackee*)
            (attempt-hit! this attackee*)))))
    (let [destination* (destination this)]
      (when-not (v3/vempty? destination*)
        (look-towards-position! this destination*)
        (move-towards-position! this destination*)))))

(def hooks
  {:start #'start!
   :update #'update!})
