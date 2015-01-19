(ns src.battle-arena.components
  (:use arcadia.core)
  (:require [arcadia.hydrate :as h])
  (:import [
            UnityEngine
            Application
            Physics
            Ray
            RaycastHit
            Mathf
            Debug
            Camera
            GameObject
            Collision
            Time
            Shader
            Transform
            LightType
            Vector3
            Material
            Input
            KeyCode
            Color
            ]))

(defn v*scalar ^Vector3 [^Vector3 v s] (Vector3/op_Multiply v (float s)))
(defn v*v ^Vector3 [^Vector3 v1 ^Vector3 v2] (Vector3/Scale v1 v2))

(defn *2v ^Vector3 [a b]
  (if (instance? Vector3 a)
    (if (instance? Vector3 b) (v*v a b) (v*scalar a b))
    (if (instance? Vector3 b) (v*scalar b a) (* a b))))

(defn *v [& xs] (reduce *2v xs))

(defn screen-size [] [(Screen/width) (Screen/height)])

(defn find-object [s] (. GameObject (Find s)))

(defn main-camera [] (UnityEngine.Camera/main))

(defn height [object] (.. object renderer bounds size y))

(defcomponent HeroComponent [^float movement-speed]
  (Awake [this]
    (Debug/Log "awake"))
  (Start [this]
    (Debug/Log "start"))
  (Update [this]
    (cond
      (Input/GetKey KeyCode/UpArrow) (.Translate (.. this transform)
                                                 (*v movement-speed
                                                     Time/deltaTime
                                                     Vector3/forward))
      (Input/GetKey KeyCode/DownArrow) (.Translate (.. this transform)
                                                   (*v movement-speed
                                                       Time/deltaTime
                                                       Vector3/back))
      (Input/GetKey KeyCode/RightArrow) (.Translate (.. this transform)
                                                    (*v movement-speed
                                                        Time/deltaTime
                                                        Vector3/right))
      (Input/GetKey KeyCode/LeftArrow) (.Translate (.. this transform)
                                                   (*v movement-speed
                                                       Time/deltaTime
                                                       Vector3/left))))
  (OnCollisionEnter [this collision]))

(defn create-hero [movement-speed]
  (let [hero (-> (create-primitive :capsule)
                 (#(h/populate!
                     %
                     {:name "Hero"
                      :transform [{:local-position [0 (/ (height %) 2) 0]
                                   :local-rotation [0 0 0 0]
                                   :local-scale [1 1 1]}]}))
                 (add-component HeroComponent))]
    (set! (.. hero (GetComponent HeroComponent) movement-speed) movement-speed)
    hero))

(defn create-terrain []
  (-> (create-primitive :plane)
      (#(h/populate!
          %
          {:name "Terrain"
           :transform [{:local-position [0 0 0]
                        :local-rotation [0 0 0 0]
                        :local-scale [5 1 5]}]}))))

(defn start []
  #_(Debug/Log "starting")
  #_(create-hero)
  #_(create-terrain))

(defn stop []
  #_(Debug/Log "stopping")
  (dorun (map destroy (objects-named "Hero")))
  (dorun (map destroy (objects-named "Light")))
  (dorun (map destroy (objects-named "Terrain"))))

(defn reset []
  (stop)
  #_(start))

(reset)

(def hero-material (Material. (Shader/Find "Specular")))
(set! (.color hero-material) Color/blue)

(def terrain-material (Material. (Shader/Find "Specular")))
(set! (.color terrain-material) Color/green)

(def hero (create-hero 50))
(set! (.. hero renderer material) hero-material)

(def terrain (create-terrain))
(set! (.. terrain renderer material) terrain-material)

(def light (GameObject. "Light"))
(add-component light "Light")

(h/populate! light {:transform [{:local-position [0 5 0]
                                 :local-rotation [0.4 0 0 0.9]}]
                    :light [{:type LightType/Directional
                             :intensity 0.4
                             :color Color/white}]})

(set! (.. terrain renderer material) terrain-material)

;; (add-component hero HeroComponent)

;; (h/dehydrate hero)

;; (.Translate (.. hero transform))
;;
;; (.Translate (.. hero transform) Vector3/back)

;; (Camera/allCameras)
;;
;; (Camera/current)
