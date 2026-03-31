package com.carddemo.common;

import java.util.List;

/**
 * Static menu configuration derived from COMEN02Y (main menu) and COADM02Y (admin menu).
 * Replaces the hardcoded COBOL FILLER tables with a queryable Java structure.
 */
public final class MenuConfig {

    private MenuConfig() {}

    public static final List<MenuOption> MAIN_MENU_OPTIONS = List.of(
        new MenuOption(1,  "Account View",               "/account/view",       MenuOption.ROLE_USER),
        new MenuOption(2,  "Account Update",             "/account/update",     MenuOption.ROLE_USER),
        new MenuOption(3,  "Credit Card List",           "/card/list",          MenuOption.ROLE_USER),
        new MenuOption(4,  "Credit Card View",           "/card/detail",        MenuOption.ROLE_USER),
        new MenuOption(5,  "Credit Card Update",         "/card/update",        MenuOption.ROLE_USER),
        new MenuOption(6,  "Transaction List",           "/transaction/list",   MenuOption.ROLE_USER),
        new MenuOption(7,  "Transaction View",           "/transaction/list",   MenuOption.ROLE_USER),
        new MenuOption(8,  "Transaction Add",            "/transaction/add",    MenuOption.ROLE_USER),
        new MenuOption(9,  "Transaction Reports",        "/report/submit",      MenuOption.ROLE_USER),
        new MenuOption(10, "Bill Payment",               "/payment/bill",       MenuOption.ROLE_USER),
        new MenuOption(11, "Pending Authorization View", "/auth/pending",       MenuOption.ROLE_USER)
    );

    public static final List<MenuOption> ADMIN_MENU_OPTIONS = List.of(
        new MenuOption(1, "User List (Security)",                    "/admin/user/list",   MenuOption.ROLE_ADMIN),
        new MenuOption(2, "User Add (Security)",                     "/admin/user/add",    MenuOption.ROLE_ADMIN),
        new MenuOption(3, "User Update (Security)",                  "/admin/user/update", MenuOption.ROLE_ADMIN),
        new MenuOption(4, "User Delete (Security)",                  "/admin/user/delete", MenuOption.ROLE_ADMIN),
        new MenuOption(5, "Transaction Type List/Update (Db2)",      "/admin/trantype/list",   MenuOption.ROLE_ADMIN),
        new MenuOption(6, "Transaction Type Maintenance (Db2)",      "/admin/trantype/update", MenuOption.ROLE_ADMIN),
        new MenuOption(7, "Phase 5 batch jobs (purge / IMS / COBTUPDT)", "/admin/batch",       MenuOption.ROLE_ADMIN)
    );

    public static List<MenuOption> optionsForRole(String role) {
        if (MenuOption.ROLE_ADMIN.equals(role)) {
            return ADMIN_MENU_OPTIONS;
        }
        return MAIN_MENU_OPTIONS;
    }
}
