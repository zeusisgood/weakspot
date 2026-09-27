package io.github.zeusisgood.weakspot.compat;

import static org.junit.Assert.assertTrue;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.server.HitHandlers;
import org.junit.Test;

public class HitHandlersTest {

    /** どの種類のヒットにも、サーバーの処理がある（種類を足したときの登録忘れを防ぐ）。 */
    @Test
    public void everyKindHasAHandler() {
        for (HitKind kind : HitKind.values()) {
            assertTrue(kind.name(), HitHandlers.has(kind));
        }
    }
}
