package dev.tutien;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Mau vanilla lam goc: MAX_HEALTH = HP RPG (qua 1 attribute modifier rieng), mau hien tai = player.getHealth().
 * Thanh HP chi doc tu hai gia tri nay. Chet / thong bao chet / totem / buff mau cua plugin khac deu la vanilla.
 */
public class HpManager {
    private final TuTienPlugin pl;
    private final NamespacedKey key;
    private boolean internal;
    private boolean internalHeal;
    private boolean warned;

    public HpManager(TuTienPlugin pl) {
        this.pl = pl;
        this.key = new NamespacedKey(pl, "rpg_max_health");
    }

    /** Max HP muc tieu theo canh gioi / tang / the chat. */
    public double targetMax(PlayerData d) {
        double bonus = 1 + pl.getConfig().getDouble("hp.stage-bonus", 0.05) * Math.max(0, d.stage - 1);
        return Realm.baseHp(d.realm) * bonus * Roots.body(d.theChat).hpMul();
    }

    /** Max HP thuc te (da gom buff cua plugin khac). */
    public double maxHp(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
        return a == null ? 20.0 : a.getValue();
    }

    public boolean isInternal() { return internal; }

    /** He so sat thuong dau ra theo HP RPG muc tieu (khong tinh buff mau cua plugin khac). */
    public double outputMultiplier(Player attacker) {
        double f = pl.getConfig().getDouble("damage-output.factor", 0.2);
        return Math.max(1.0, targetMax(pl.data().get(attacker)) / 20.0 * f);
    }

    /** Sat thuong 1 don can ban cua nguoi choi (dung quy doi vo ky). */
    public double baseHit(Player attacker) {
        return pl.getConfig().getDouble("damage-output.base-hit", 7.0) * outputMultiplier(attacker);
    }

    /** Gay sat thuong len thuc the bat ky (quai/nguoi choi), da tinh san -> khong bi nhan he so lan nua. */
    public void hurtEntity(org.bukkit.entity.LivingEntity le, double amount, DamageSource src) {
        if (le.isDead() || amount <= 0) return;
        internal = true;
        try {
            le.setNoDamageTicks(0);
            le.damage(amount, src);
        } finally {
            internal = false;
        }
    }

    public boolean isInternalHeal() { return internalHeal; }

    /** Dat MAX_HEALTH = targetMax (cong them modifier). keepRatio: giu nguyen % mau hien tai. */
    public void applyMax(Player p, boolean keepRatio) {
        AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
        if (a == null) return;
        double oldMax = a.getValue();
        double ratio = oldMax <= 0 ? 1 : p.getHealth() / oldMax;
        for (AttributeModifier m : a.getModifiers()) {
            if (key.equals(m.getKey())) a.removeModifier(m);
        }
        double target = targetMax(pl.data().get(p));
        a.addModifier(new AttributeModifier(key, target - a.getBaseValue(),
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        double newMax = a.getValue();
        if (newMax + 1 < target && !warned) {
            warned = true;
            pl.getLogger().warning("MAX_HEALTH bi gioi han o " + newMax + " < " + target
                    + ". Tang settings.attribute.maxHealth.max trong spigot.yml (vd 1000000) roi restart.");
        }
        if (keepRatio && !p.isDead()) p.setHealth(Math.max(0.5, Math.min(newMax, ratio * newMax)));
    }

    /** Goi khi vao server: ap max, khoi phuc mau tu du lieu luu, bat health scale cho client. */
    public void onJoin(Player p) {
        PlayerData d = pl.data().get(p);
        applyMax(p, false);
        double max = maxHp(p);
        double want = d.hp > 0 ? d.hp : max;
        if (!p.isDead()) p.setHealth(Math.max(0.5, Math.min(max, want)));
        p.setHealthScale(20.0);
        p.setHealthScaled(true);
    }

    public void heal(Player p, double amount) {
        if (p.isDead() || amount <= 0) return;
        // Hoi mau qua EntityRegainHealthEvent (CUSTOM) de plugin khac thay/huy duoc; khong bi nhan regen-scale
        internalHeal = true;
        try {
            p.heal(amount, org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.CUSTOM);
        } finally {
            internalHeal = false;
        }
    }

    public void healPct(Player p, double pct) { heal(p, maxHp(p) * pct); }

    public void fill(Player p) {
        if (!p.isDead()) p.setHealth(maxHp(p));
    }

    /** Gay sat thuong vanilla (qua giap) tu plugin - khong bi nhan damage-scale. */
    public void hurt(Player p, double amount, DamageSource src) {
        if (p.isDead() || amount <= 0) return;
        internal = true;
        try {
            p.damage(amount, src);
        } finally {
            internal = false;
        }
    }

    /** Sat thuong "chuan" (MAGIC bo qua giap). */
    public void magic(Player p, double amount) {
        hurt(p, amount, DamageSource.builder(DamageType.MAGIC).build());
    }
}
