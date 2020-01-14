using System;
using UnityEngine;
using System.Collections;

namespace Felucca.Components {
    public class Creature : MonoBehaviour {
        public int strength;
        public int dexterity;
        public int intelligence;
        
        public int hitPoints;
        public int stamina;
        public int mana;
        
        public int movementSpeed;
        public int rotationSpeed;
        public int attackSpeed;
        
        public int attackRange;
        
        public Vector3 destination;
        
        public GameObject attackee;
        public GameObject[] threateners;
        public float lastHitAttemptedAt;
        
        ////////////////////////////////////////////////////////////////////////
        // Lifecycle ///////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        private void Awake() {
            strength = 50;
            rotationSpeed = 50;
            movementSpeed = 50;
            hitPoints = strength;
            destination = transform.localPosition;
        }

        private void Update() {
            if (IsAlive()) {
                if (!IsAtDestination()) {
                    LookTowardsPosition(destination);
                    MoveTowardsPosition(destination);
                }
            }
        }
        
        ////////////////////////////////////////////////////////////////////////
        // Actions /////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public void LookTowardsPosition(Vector3 position) {
            var currentPosition = transform.localPosition;
            var currentRotation = transform.localRotation;
            var targetRotation = Quaternion.LookRotation(
                Vector3.Scale(
                    new Vector3(1, 0, 1),
                    position - currentPosition
                )
            );
            transform.localRotation = Quaternion.RotateTowards(
                currentRotation,
                targetRotation,
                NormalizedRotationSpeed() * Time.deltaTime
            );
        }

        public void MoveTowardsPosition(Vector3 position) {
            if ((transform.localPosition - position).magnitude > 0.5) {
                GetComponent<CharacterController>().SimpleMove(
                    transform.forward * NormalizedMovementSpeed()
                );
            }
        }

        public void SetDestination(Vector3 _destination) {
            destination = _destination;
        }
        
        ////////////////////////////////////////////////////////////////////////
        // Domain logic ////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public bool IsAlive() {
            return hitPoints > 0;
        }

        public bool IsDead() {
            return !IsAlive();
        }

        public bool IsAtDestination() {
            return destination == transform.position;
        }

        private float NormalizedRotationSpeed() {
            return rotationSpeed * 10f;
        }
        
        private float NormalizedMovementSpeed() {
            return movementSpeed / 10f;
        }
    }
}