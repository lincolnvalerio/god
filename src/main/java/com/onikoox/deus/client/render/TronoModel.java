package com.onikoox.deus.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Corpo de jogador com a pose de rei no trono: costas eretas, pernas abertas com os pés no chão e as mãos
 * apoiadas nos braços do trono. Também faz os gestos de conjuração (em pé: dois braços; sentado: só a mão direita).
 * Os valores abaixo são ajustáveis.
 */
public class TronoModel<T extends LivingEntity> extends PlayerModel<T> {
    /** 0 = em pé, 1 = pose completa do trono (animado entre os dois ao sentar e levantar). */
    public float sentado = 0F;
    /** 0..1 = intensidade do gesto de conjuração. */
    public float gesto = 0F;
    public boolean gestoSentado = false;

    public TronoModel(ModelPart raiz, boolean magro) {
        super(raiz, magro);
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        this.riding = false; // a pose sentada de vanilla (pernas esticadas) é desligada; usamos a nossa
        super.setupAnim(e, limbSwing, limbSwingAmount, age, headYaw, headPitch);

        float k = Mth.clamp(sentado, 0F, 1F);
        if (k > 0.001F) {
            // pernas abertas, pés no chão (xRot negativo = para a frente; zRot + abre a perna direita, - a esquerda)
            rightLeg.xRot = Mth.lerp(k, rightLeg.xRot, -0.80F);
            leftLeg.xRot = Mth.lerp(k, leftLeg.xRot, -0.80F);
            rightLeg.zRot = Mth.lerp(k, rightLeg.zRot, 0.30F);
            leftLeg.zRot = Mth.lerp(k, leftLeg.zRot, -0.30F);
            rightLeg.yRot = Mth.lerp(k, rightLeg.yRot, 0F);
            leftLeg.yRot = Mth.lerp(k, leftLeg.yRot, 0F);
            // mãos apoiadas nos braços do trono
            rightArm.xRot = Mth.lerp(k, rightArm.xRot, -1.00F);
            leftArm.xRot = Mth.lerp(k, leftArm.xRot, -1.00F);
            rightArm.zRot = Mth.lerp(k, rightArm.zRot, 0.10F);
            leftArm.zRot = Mth.lerp(k, leftArm.zRot, -0.10F);
            rightArm.yRot = Mth.lerp(k, rightArm.yRot, 0F);
            leftArm.yRot = Mth.lerp(k, leftArm.yRot, 0F);
            // costas eretas
            body.xRot = Mth.lerp(k, body.xRot, 0F);
        }

        float g = Mth.clamp(gesto, 0F, 1F);
        if (g > 0.001F) {
            float osc = Mth.sin(age * 0.45F) * 0.06F * g;
            if (gestoSentado) {
                rightArm.xRot = Mth.lerp(g, rightArm.xRot, -1.55F + osc);
                rightArm.zRot = Mth.lerp(g, rightArm.zRot, 0.45F);
            } else {
                rightArm.xRot = Mth.lerp(g, rightArm.xRot, -1.75F + osc);
                leftArm.xRot = Mth.lerp(g, leftArm.xRot, -1.75F - osc);
                rightArm.zRot = Mth.lerp(g, rightArm.zRot, 0.45F);
                leftArm.zRot = Mth.lerp(g, leftArm.zRot, -0.45F);
            }
        }

        // as camadas externas (mangas, calças, casaco) precisam acompanhar
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        jacket.copyFrom(body);
    }
}
