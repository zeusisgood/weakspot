package com.example.weakspot.compat;

import static org.junit.Assert.assertTrue;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.server.HitHandlers;
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
