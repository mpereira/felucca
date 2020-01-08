(ns felucca.components.following-camera
  (:require [arcadia.core :refer [defmutable log update-state state]]
            [felucca.vector3 :as v3])
  (:import [UnityEngine Vector3]))

(defmutable FollowingCamera [^GameObject followee
                             ^Vector3 followee-offset])

(defn start! [^GameObject game-object role-key]
  (update-state game-object
                :following-camera
                (fn [{:keys [followee] :as following-camera-state}]
                  (assoc following-camera-state
                         :followee-offset
                         (v3/v- (.. game-object transform position)
                                (.. followee transform position))))))

(defn late-update! [^GameObject game-object role-key]
  (let [following-camera-state (state game-object :following-camera)]
    (set! (.. game-object transform position)
          (.. (:followee following-camera-state) transform position))))

(def hooks
  {:start #'start!
   :late-update #'late-update!})
