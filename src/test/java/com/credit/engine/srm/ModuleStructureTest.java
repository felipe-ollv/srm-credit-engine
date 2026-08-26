package com.credit.engine.srm;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleStructureTest {

    @Test
    void shouldRespectApplicationModuleBoundaries() {
        ApplicationModules.of(SrmApplication.class).verify();
    }
}
