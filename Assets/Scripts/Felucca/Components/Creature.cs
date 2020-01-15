using UnityEngine;
using System.Linq;
using JetBrains.Annotations;

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
        
        [CanBeNull] public Creature attackee;
        public Creature[] threateners;
        public float? lastHitAttemptedAt;

        public CharacterController characterController;
        public DragCreatureBar dragCreatureBar;
        
        ////////////////////////////////////////////////////////////////////////
        // Lifecycle ///////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        private void Start() {
            strength = 50;
            rotationSpeed = 50;
            movementSpeed = 50;
            attackSpeed = 50;
            attackRange = 50;
            hitPoints = strength;
            destination = transform.localPosition;
            characterController = GetComponent<CharacterController>();
            dragCreatureBar = gameObject.AddComponent<DragCreatureBar>();
        }

        private void Update() {
            if (IsAlive()) {
                if (!IsAtDestination()) {
                    LookTowardsPosition(destination);
                    MoveTowardsPosition(destination);
                }
                if (attackee != null && attackee.IsAlive()) {
                    SetDestination(attackee.transform.localPosition);
                    if (WithinAttackRange(attackee)) {
                        AttemptHit(attackee);
                    }
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
                characterController.SimpleMove(
                    transform.forward * NormalizedMovementSpeed()
                );
            }
        }

        public void StartAttacking(Creature anotherCreature) {
            if (!ReferenceEquals(anotherCreature, this)) {
                attackee = anotherCreature;
            }
        }

        public void StopAttacking() {
            attackee = null;
        }

        public void AcknowledgeAttacker(Creature attackingCreature) {
            StartAttacking(attackingCreature);
            threateners.Append<Creature>(attackingCreature);
        }

        public void Die() {
        }

        public void ReceiveHit(int damage) {
            hitPoints = Mathf.Clamp(hitPoints - damage, 0, MaxHitPoints());
            if (IsDead()) {
                Die();
            }
        }

        public void Hit(Creature anotherCreature) {
            lastHitAttemptedAt = Time.time;
            anotherCreature.ReceiveHit(5);
        }
        
        public void AttemptHit(Creature anotherCreature) {
            if (IsRecoveredFromPreviousHit()) {
                Hit(anotherCreature);
            }
        }

        public void SetDestination(Vector3 _destination) {
            destination = _destination;
        }
        
        ////////////////////////////////////////////////////////////////////////
        // Domain logic ////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public float Distance(Creature anotherCreature) {
            return Vector3.Distance(
                transform.localPosition,
                anotherCreature.gameObject.transform.localPosition
            );
        }

        public bool WithinAttackRange(Creature anotherCreature) {
            return NormalizedAttackRange() > Distance(anotherCreature);
        }

        public bool IsRecoveredFromPreviousHit() {
            return lastHitAttemptedAt == null ||
                   Time.time - lastHitAttemptedAt > NormalizedAttackSpeed();
        }

        public int MaxHitPoints() {
            return strength;
        }

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
        
        private float NormalizedAttackSpeed() {
            return attackSpeed / 10f;
        }
        
        private float NormalizedAttackRange() {
            return attackRange / 25f;
        }
    }
}