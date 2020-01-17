using System;
using UnityEngine;
using Random = UnityEngine.Random;

namespace Felucca.Components {
    public class StatAndSkillSystem : MonoBehaviour {
        public Creature creature;

        private void Start() {
            creature.onHitAttempted += MaybeIncreaseStat("strength");
            creature.onHitAttempted += MaybeIncreaseStat("dexterity");
            creature.onHitAttempted += MaybeIncreaseSkill("wrestling");
        }

        private float StatIncreaseChance(String stat) {
            // Make this depend on stat, etc.
            return 0.1f;
        }

        private float SkillIncreaseChance(String skill) {
            // Make this depend on skill, etc..
            return 0.1f;
        }

        private Action MaybeIncreaseStat(String stat) {
            return () => {
                if (StatIncreaseChance(stat) > Random.value) {
                    creature.IncreaseStat(stat, 1);
                }
            };
        }

        private Action MaybeIncreaseSkill(String skill) {
            return () => {
                if (SkillIncreaseChance(skill) > Random.value) {
                    creature.IncreaseSkill(skill, 0.1f);
                }
            };
        }
    }
}