(ns battle-arena.utils
  (:use arcadia.core)
  (:require [battle-arena.vector3 :refer :all])
  (:import IEnumerator
           Timeline
           [UnityEngine
            Application
            Camera
            CharacterController
            Debug
            KeyCode
            Mathf
            Physics
            Renderer
            Ray
            RaycastHit
            Time]))

(defn ->seq [ary]
  (let [e (.GetEnumerator ary)]
    (loop [coll '()]
      (if (.MoveNext e)
        (recur (cons (.Current e) coll))
        coll))))

(defn find-object [s] (. GameObject (Find s)))

(defn main-camera [] (UnityEngine.Camera/main))

(defn height [object]
  (with-cmpt object [r Renderer]
    (.. r bounds size y)))

(defmulti terrain? class)

(defmethod terrain? RaycastHit [raycast-hit]
  (= "Terrain" (.. raycast-hit transform gameObject name)))

(defmulti enemy-hero? class)

(defmethod enemy-hero? RaycastHit [raycast-hit]
  (= "Enemy Hero" (.. raycast-hit transform gameObject name)))

(defn target-hit []
  (let [ray (.ScreenPointToRay Camera/main Input/mousePosition)
        hits (->seq (Physics/RaycastAll ray
                                        Mathf/Infinity
                                        Physics/DefaultRaycastLayers))]
    (last hits)))

(defn after [seconds f]
  (let [waited? (atom false)
        ^WaitForSeconds wait-for-seconds (WaitForSeconds. seconds)]
    (.. (Camera/main)
        (GetComponent Camera)
        (StartCoroutine (reify
                          IEnumerator
                          (MoveNext [this]
                            (let [continue? (not @waited?)]
                              (when @waited? (f))
                              (reset! waited? true)
                              continue?))
                          (get_Current [this]
                            wait-for-seconds))))))
