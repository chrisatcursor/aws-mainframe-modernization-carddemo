package com.carddemo.common;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MenuConfigTest {

    @Test
    void mainMenu_has11Options() {
        assertThat(MenuConfig.MAIN_MENU_OPTIONS).hasSize(11);
    }

    @Test
    void adminMenu_has6Options() {
        assertThat(MenuConfig.ADMIN_MENU_OPTIONS).hasSize(6);
    }

    @Test
    void mainMenu_firstOption_isAccountView() {
        MenuOption first = MenuConfig.MAIN_MENU_OPTIONS.get(0);
        assertThat(first.number()).isEqualTo(1);
        assertThat(first.name()).isEqualTo("Account View");
        assertThat(first.endpoint()).isEqualTo("/account/view");
        assertThat(first.requiredRole()).isEqualTo(MenuOption.ROLE_USER);
    }

    @Test
    void adminMenu_allOptions_requireAdminRole() {
        assertThat(MenuConfig.ADMIN_MENU_OPTIONS)
            .allMatch(opt -> MenuOption.ROLE_ADMIN.equals(opt.requiredRole()));
    }

    @Test
    void optionsForRole_admin_returnsAdminMenu() {
        List<MenuOption> options = MenuConfig.optionsForRole(MenuOption.ROLE_ADMIN);
        assertThat(options).isEqualTo(MenuConfig.ADMIN_MENU_OPTIONS);
    }

    @Test
    void optionsForRole_user_returnsMainMenu() {
        List<MenuOption> options = MenuConfig.optionsForRole(MenuOption.ROLE_USER);
        assertThat(options).isEqualTo(MenuConfig.MAIN_MENU_OPTIONS);
    }
}
