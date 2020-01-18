using System;
using UnityEngine;
using System.Linq;
using JetBrains.Annotations;
using Random = UnityEngine.Random;

namespace Felucca.Components {
    public class Creature : MonoBehaviour {
        // Stats.
        public int strength;
        public int dexterity;
        public int intelligence;

        // Attributes.
        public int hitPoints;
        public int stamina;
        public int mana;

        // Skills.
        public double wrestling;

        public int movementSpeed;
        public int rotationSpeed;
        public int attackSpeed;

        public int attackRange;

        public Vector3 destination;

        [CanBeNull] public Creature   attackee;
        public             Creature[] threateners;
        public             float?     lastHitAttemptedAt;

        public CharacterController characterController;

        public float moveThreshold = 0.5f;

        public event Action OnHitAttempted;
        public event Action OnHit;
        public event Action OnMiss;
        public event Action OnDeath;

        ////////////////////////////////////////////////////////////////////////
        // Lifecycle ///////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        private void Start() {
            hitPoints = strength;
            stamina = dexterity;
            mana = intelligence;

            destination = transform.localPosition;

            characterController = GetComponent<CharacterController>();
        }

        private void Update() {
            if (IsAlive()) {
                if (!IsAtDestination()) {
                    LookTowardsPosition(destination);
                    MoveTowardsPosition(destination);
                }

                if (attackee != null && attackee.IsAlive()) {
                    StartMovingTowards(attackee.transform.localPosition);
                    if (WithinAttackRange(attackee)) {
                        AttemptHit(attackee);
                    }
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////
        // Actions /////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public int IncreaseStat(String stat, int increase) {
            switch (stat) {
                case "strength": {
                    strength += increase;
                    return strength;
                }
                case "dexterity": {
                    dexterity += increase;
                    return dexterity;
                }
                case "intelligence": {
                    intelligence += increase;
                    return intelligence;
                }
                default: return 0;
            }
        }

        public double IncreaseSkill(String skill, double increase) {
            switch (skill) {
                case "wrestling": {
                    wrestling += increase;
                    return Math.Round(wrestling, 2);
                }
                default: return 0f;
            }
        }

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
            if (Distance(position) > moveThreshold) {
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

        public void Resurrect() {
        }

        public void ReceiveHit(int damage) {
            hitPoints = Mathf.Clamp(hitPoints - damage, 0, MaxHitPoints());
            if (IsDead()) {
                Die();
            }
        }

        public void Heal(int amount) {
            var wasDead = IsDead();
            hitPoints = Mathf.Clamp(hitPoints + amount, 0, MaxHitPoints());
            if (wasDead && IsAlive()) {
                Resurrect();
            }
        }

        // http://www.uoguide.com/Damage_Increase
        // https://uo.stratics.com/content/arms-armor/damage.php
        public int Damage() {
            // TODO: make it depend on stats, skills, equips, etc.
            return 5;
        }

        public void Hit(Creature anotherCreature) {
            lastHitAttemptedAt = Time.time;
            anotherCreature.ReceiveHit(Damage());
        }

        public void Miss(Creature anotherCreature) {
        }

        public float HitChance(Creature anotherCreature) {
            // TODO: make this depend on stats, skills, etc.
            return 0.5f;
        }

        public void AttemptHit(Creature anotherCreature) {
            if (IsRecoveredFromPreviousHit()) {
                OnHitAttempted?.Invoke();
                if (HitChance(anotherCreature) > Random.value) {
                    Hit(anotherCreature);
                } else {
                    Miss(anotherCreature);
                }
            }
        }

        public void StartMovingTowards(Vector3 destination) {
            this.destination = destination;
        }

        public void StopMoving() {
            destination = transform.localPosition;
        }

        ////////////////////////////////////////////////////////////////////////
        // Domain logic ////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////

        public float Distance(Vector3 position) {
            return Vector3.Distance(transform.localPosition, position);
        }

        public float Distance(Creature anotherCreature) {
            return Distance(anotherCreature.gameObject.transform.localPosition);
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
            return attackRange / 20f;
        }
    }
}
