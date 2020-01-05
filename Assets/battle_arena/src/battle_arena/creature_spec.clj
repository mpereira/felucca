(ns battle-arena.creature-spec
  (:require [battle-arena.components.creature :as creature]
            [battle-arena.vector3 :as v3]))

(defn range->attribute [[min* max*]]
  (+ min* (rand-int (+ 1 (- max* min*)))))

(defn spec->creature
  [{:keys [movement-speed
           rotation-speed]
    creature-name :name
    {:keys [scale]} :ui
    {:keys [strength dexterity intelligence]} :stats
    {:keys [attack-speed attack-range base-damage max-critical-hit-chance]} :attack
    :as spec}]
  (creature/->Creature creature-name
                       (range->attribute strength)
                       (range->attribute dexterity)
                       (range->attribute intelligence)
                       0
                       0
                       0
                       movement-speed
                       rotation-speed
                       attack-speed
                       attack-range
                       (v3/vempty)
                       (v3/vempty)
                       nil
                       0.0
                       []))
