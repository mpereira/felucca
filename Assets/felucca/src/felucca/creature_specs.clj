(ns felucca.creature-specs
  (:require [felucca.vector3 :as v3])
  (:import (UnityEngine Color)))

(def rat
  {:name "a rat"
   :alignment :neutral
   :movement-speed 30
   :rotation-speed 30
   :aggressiveness-radius 100
   :stats {:strength [10 15]
           :dexterity [10 15]
           :intelligence [5 10]}
   :skills {:wrestling [20 30]}
   :resistances {:physical nil
                 :fire nil
                 :cold nil
                 :poison nil
                 :energy nil}
   :attack {:attack-speed 20
            :attack-range 50
            :base-damage [2 5]
            :max-critical-hit-chance 20}
   :items {:gold [5 10]}
   :ui {:scale (v3/v3 0.5 0.5 0.5)
        :color (Color/gray)}})

(def dragon
  {:name "a dragon"
   :alignment :evil
   :movement-speed 60
   :rotation-speed 60
   :aggressiveness-radius 200
   :stats {:strength [800 850]
           :dexterity [80 100]
           :intelligence [430 480]}
   :skills {:wrestling 100}
   :resistances {:physical nil
                 :fire nil
                 :cold nil
                 :poison nil
                 :energy nil}
   :attack {:base-damage [2 5]
            :attack-speed 80
            :attack-range 200
            :max-critical-hit-chance 80}
   :items {:gold [600 800]}
   :ui {:scale (v3/v3 2 4 2)
        :color (Color/red)}})
