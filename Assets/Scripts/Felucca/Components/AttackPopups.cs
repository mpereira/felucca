using TMPro;
using UnityEngine;

namespace Felucca.Components {
    public class AttackPopups : MonoBehaviour {
        public GameObject hitPopupPrefab;
        public GameObject missPopupPrefab;
        public GameObject attackPopupsContainer;
        public int        attackPopupFontSize;
        public int        attackPopupTtl;
        public float      attackPopupDisappearSpeed;

        public void RegisterCreature(Creature creature) {
            Debug.Log("registering " + creature.name);
            creature.OnHit += DisplayHit;
            creature.OnMiss += DisplayMiss;
        }

        public void DisplayHit(
            Creature attacker, Creature attackee, int damage
        ) {
            var popupGameObject = Instantiate(
                hitPopupPrefab,
                attackPopupsContainer.transform,
                true
            );

            var popupText = popupGameObject.GetComponent<TextMeshProUGUI>();
            popupText.text = damage.ToString();
            popupText.fontSize = attackPopupFontSize;

            var attackPopup = popupGameObject.GetComponent<AttackPopup>();
            attackPopup.Follow(attackee);

            var fadingVisual = popupGameObject.GetComponent<FadingVisual>();
            fadingVisual.SetTtlAndDisappearSpeed(
                attackPopupTtl,
                attackPopupDisappearSpeed
            );

            Destroy(
                popupGameObject,
                attackPopupTtl + attackPopupDisappearSpeed
            );
        }

        public void DisplayMiss(Creature attacker, Creature attackee) {
            var popupGameObject = Instantiate(
                missPopupPrefab,
                attackPopupsContainer.transform,
                true
            );

            var popupText = popupGameObject.GetComponent<TextMeshProUGUI>();
            popupText.text = "miss";
            popupText.fontSize = attackPopupFontSize;

            var attackPopup = popupGameObject.GetComponent<AttackPopup>();
            attackPopup.Follow(attackee);

            var fadingVisual = popupGameObject.GetComponent<FadingVisual>();
            fadingVisual.SetTtlAndDisappearSpeed(
                attackPopupTtl,
                attackPopupDisappearSpeed
            );

            Destroy(
                popupGameObject,
                attackPopupTtl + attackPopupDisappearSpeed
            );
        }
    }
}
