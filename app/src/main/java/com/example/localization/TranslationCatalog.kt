package com.example.localization

import org.json.JSONArray
import org.json.JSONObject

object TranslationCatalog {

    val defaultEntries: List<TranslationItemSpec> = listOf(
        // Brand & App
        TranslationItemSpec("app_brand_name", TranslationCategory.BRAND, "LAKEMARK", "ሌክማርክ"),
        TranslationItemSpec("app_subtitle", TranslationCategory.BRAND, "Kitchen Management", "የወጥ ቤት አስተዳደር"),
        TranslationItemSpec("app_version_footer", TranslationCategory.BRAND, "Lakemark v{ver} · Bahir Dar", "ሌክማርክ v{ver} · ባሕር ዳር"),
        TranslationItemSpec("currency_etb", TranslationCategory.BRAND, "ETB {amount}", "{amount} ብር"),
        TranslationItemSpec("brand_location", TranslationCategory.BRAND, "Bahir Dar", "ባሕር ዳር"),

        // Navigation
        TranslationItemSpec("nav_home", TranslationCategory.NAV, "Home", "መነሻ"),
        TranslationItemSpec("nav_floor", TranslationCategory.NAV, "Floor", "ወለል"),
        TranslationItemSpec("nav_menu", TranslationCategory.NAV, "Menu", "ምናሌ"),
        TranslationItemSpec("nav_saved", TranslationCategory.NAV, "Saved", "የተቀመጡ"),
        TranslationItemSpec("nav_orders", TranslationCategory.NAV, "Orders", "ትዕዛዞች"),
        TranslationItemSpec("nav_kitchen", TranslationCategory.NAV, "Kitchen", "ወጥ ቤት"),
        TranslationItemSpec("nav_manage", TranslationCategory.NAV, "Manage", "አስተዳድር"),
        TranslationItemSpec("btn_back", TranslationCategory.NAV, "Back", "ተመለስ"),
        TranslationItemSpec("btn_close", TranslationCategory.NAV, "Close", "ዝጋ"),
        TranslationItemSpec("btn_cancel", TranslationCategory.NAV, "Cancel", "ሰርዝ"),
        TranslationItemSpec("btn_save", TranslationCategory.NAV, "Save", "አስቀምጥ"),
        TranslationItemSpec("btn_reset", TranslationCategory.NAV, "Reset", "ዳግም አስጀምር"),
        TranslationItemSpec("btn_confirm", TranslationCategory.NAV, "Confirm", "አረጋግጥ"),
        TranslationItemSpec("btn_later", TranslationCategory.NAV, "Later", "በኋላ"),
        TranslationItemSpec("btn_got_it", TranslationCategory.NAV, "Got it", "ገባኝ"),

        // Auth & Access
        TranslationItemSpec("btn_get_started", TranslationCategory.AUTH, "Get Started", "ይጀምሩ"),
        TranslationItemSpec("auth_welcome", TranslationCategory.AUTH, "Welcome to Lakemark", "ወደ ሌክማርክ እንኳን በደህና መጡ"),
        TranslationItemSpec("auth_tab_staff", TranslationCategory.AUTH, "Staff Sign In", "የሰራተኞች መግቢያ"),
        TranslationItemSpec("auth_tab_google", TranslationCategory.AUTH, "Manager Login", "የአስተዳዳሪ መግቢያ"),
        TranslationItemSpec(
            "auth_desc_staff",
            TranslationCategory.AUTH,
            "Staff sign in with internal Lakemark credentials created by your Manager — no Google account required.",
            "ሰራተኞች በአስተዳዳሪው በተፈጠረ የሌክማርክ መለያ ይግቡ — የGoogle መለያ አያስፈልግም።",
        ),
        TranslationItemSpec(
            "auth_desc_google",
            TranslationCategory.AUTH,
            "Only the Manager / Admin account uses Google authentication to configure Lakemark. All other staff accounts are created internally without Google login.",
            "መተግበሪያውን ለማዋቀር የGoogle ማረጋገጫ የሚያስፈልገው የአስተዳዳሪ መለያ ብቻ ነው። ሌሎች የሰራተኛ መለያዎች በሙሉ ያለ Google መግቢያ በውስጥ ይፈጠራሉ።",
        ),
        TranslationItemSpec("auth_username_label", TranslationCategory.AUTH, "Staff username", "የሰራተኛ የተጠቃሚ ስም"),
        TranslationItemSpec("auth_username_hint", TranslationCategory.AUTH, "e.g. kitchen1 or waiter_abel", "ለምሳሌ kitchen1 ወይም waiter_abel"),
        TranslationItemSpec("auth_password_label", TranslationCategory.AUTH, "Password", "የይለፍ ቃል"),
        TranslationItemSpec("auth_show_password", TranslationCategory.AUTH, "Show password", "የይለፍ ቃል አሳይ"),
        TranslationItemSpec("auth_hide_password", TranslationCategory.AUTH, "Hide password", "የይለፍ ቃል ደብቅ"),
        TranslationItemSpec("auth_staff_signin_btn", TranslationCategory.AUTH, "Sign in to station", "ወደ ስራ ክፍል ግባ"),
        TranslationItemSpec(
            "auth_staff_roles_hint",
            TranslationCategory.AUTH,
            "For Reception, Kitchen, Waiter, Cashier & Delivery staff — created under Manage → Team.",
            "ለእንግዳ መቀበያ፣ ለወጥ ቤት፣ ለአስተናጋጅ፣ ለገንዘብ ተቀባይ እና ለአድራሽ ሰራተኞች — በአስተዳድር → ቡድን ስር ይፈጠራሉ።",
        ),
        TranslationItemSpec("auth_google_btn", TranslationCategory.AUTH, "Continue with Google", "በGoogle ይቀጥሉ"),
        TranslationItemSpec(
            "auth_manager_google_note",
            TranslationCategory.AUTH,
            "All other staff accounts are created and managed internally without Google login.",
            "ሁሉም ሌሎች የሰራተኛ መለያዎች ያለ Google መግቢያ በውስጥ ይፈጠራሉ እና ይተዳደራሉ።",
        ),
        TranslationItemSpec(
            "auth_manager_only_error",
            TranslationCategory.AUTH,
            "Only an authorized Manager / Admin account can sign in with Google. Staff must use Staff Sign In.",
            "በGoogle መግባት የሚችለው የተፈቀደለት አስተዳዳሪ መለያ ብቻ ነው። ሰራተኞች በሰራተኞች መግቢያ ይግቡ።",
        ),
        TranslationItemSpec(
            "auth_customer_rule_note",
            TranslationCategory.AUTH,
            "Customers remain disabled by default until a Lakemark Manager approves and permanently activates their account.",
            "ደንበኞች በሌክማርክ አስተዳዳሪ እስኪጸድቁ እና በቋሚነት እስኪነቁ ድረስ መለያቸው በነባሪ ተዘግቶ ይቆያል።",
        ),
        TranslationItemSpec("auth_verifying_status", TranslationCategory.AUTH, "Verifying account status…", "የመለያ ሁኔታ በማረጋገጥ ላይ…"),
        TranslationItemSpec("auth_approval_pending", TranslationCategory.AUTH, "Approval Pending", "ማጽደቅ በመጠባበቅ ላይ"),
        TranslationItemSpec("auth_account_disabled", TranslationCategory.AUTH, "Account Disabled", "መለያው ተዘግቷል"),
        TranslationItemSpec(
            "auth_pending_desc",
            TranslationCategory.AUTH,
            "Customer accounts remain disabled by default until a Lakemark Manager explicitly approves and permanently activates your account. Access unlocks automatically once approved.",
            "የደንበኛ መለያዎች በሌክማርክ አስተዳዳሪ በግልጽ እስኪጸድቁ እና በቋሚነት እስኪነቁ ድረስ በነባሪ ተዘግተው ይቆያሉ። እንደጸደቁ መዳረሻው በራስ-ሰር ይከፈታል።",
        ),
        TranslationItemSpec(
            "auth_disabled_desc",
            TranslationCategory.AUTH,
            "Your staff account is currently disabled by management. Please sign out or contact a Lakemark manager.",
            "የሰራተኛ መለያዎ በአሁኑ ጊዜ በአስተዳደሩ ተዘግቷል። እባክዎ ይውጡ ወይም የሌክማርክ አስተዳዳሪን ያነጋግሩ።",
        ),
        TranslationItemSpec("btn_sign_out", TranslationCategory.AUTH, "Sign out", "ውጣ"),
        TranslationItemSpec("err_staff_signin_failed", TranslationCategory.AUTH, "Staff sign-in failed", "የሰራተኛ መግቢያ አልተሳካም"),

        // Home & Floor
        TranslationItemSpec("home_unit_orders", TranslationCategory.HOME, "orders", "ትዕዛዞች"),
        TranslationItemSpec("home_unit_waiting", TranslationCategory.HOME, "waiting", "በመጠበቅ ላይ"),
        TranslationItemSpec("home_unit_ready", TranslationCategory.HOME, "ready", "ዝግጁ"),
        TranslationItemSpec("home_unit_cooking", TranslationCategory.HOME, "cooking", "በመበሰል ላይ"),
        TranslationItemSpec("home_unit_settled", TranslationCategory.HOME, "settled", "ተጠናቋል"),
        TranslationItemSpec("home_unit_active", TranslationCategory.HOME, "active", "ንቁ"),
        TranslationItemSpec("home_sub_manager", TranslationCategory.HOME, "received by the kitchen today", "ዛሬ ወጥ ቤት የደረሱ"),
        TranslationItemSpec("home_sub_reception", TranslationCategory.HOME, "orders to send to the kitchen", "ወደ ወጥ ቤት የሚላኩ ትዕዛዞች"),
        TranslationItemSpec("home_sub_waiter", TranslationCategory.HOME, "orders waiting to be served", "ለመቅረብ የሚጠብቁ ትዕዛዞች"),
        TranslationItemSpec("home_sub_kitchen", TranslationCategory.HOME, "orders in the kitchen now", "አሁን ወጥ ቤት ውስጥ ያሉ ትዕዛዞች"),
        TranslationItemSpec("home_sub_cashier", TranslationCategory.HOME, "orders completed today", "ዛሬ የተጠናቀቁ ትዕዛዞች"),
        TranslationItemSpec("home_sub_delivery", TranslationCategory.HOME, "takeaway & delivery orders", "የሚወሰዱ እና የአድራሻ ትዕዛዞች"),
        TranslationItemSpec("home_sub_customer", TranslationCategory.HOME, "your orders in progress", "በሂደት ላይ ያሉ ትዕዛዞችዎ"),
        TranslationItemSpec("home_tag_waiting_rec", TranslationCategory.HOME, "{n} waiting at reception", "{n} በእንግዳ መቀበያ"),
        TranslationItemSpec("home_tag_cooking", TranslationCategory.HOME, "{n} cooking", "{n} በመበሰል ላይ"),
        TranslationItemSpec("home_tag_sales", TranslationCategory.HOME, "{amount} sales", "{amount} ሽያጭ"),
        TranslationItemSpec("home_tag_ready_handover", TranslationCategory.HOME, "{n} ready to hand over", "{n} ለማስረከብ ዝግጁ"),
        TranslationItemSpec("home_tag_at_reception", TranslationCategory.HOME, "{n} at reception", "{n} በእንግዳ መቀበያ"),
        TranslationItemSpec("home_tag_to_start", TranslationCategory.HOME, "{n} to start", "{n} ለመጀመር"),
        TranslationItemSpec("home_tag_ready", TranslationCategory.HOME, "{n} ready", "{n} ዝግጁ"),
        TranslationItemSpec("home_tag_still_open", TranslationCategory.HOME, "{n} still open", "{n} ክፍት"),
        TranslationItemSpec("home_tag_ready_go", TranslationCategory.HOME, "{n} ready to go", "{n} ለመሄድ ዝግጁ"),
        TranslationItemSpec("home_tag_done_today", TranslationCategory.HOME, "{n} completed today", "{n} ዛሬ ተጠናቀቁ"),
        TranslationItemSpec("home_row_received_today", TranslationCategory.HOME, "Received today", "ዛሬ የተቀበሉ"),
        TranslationItemSpec("home_row_preparing", TranslationCategory.HOME, "Preparing", "በዝግጅት ላይ"),
        TranslationItemSpec("home_row_ready_pickup", TranslationCategory.HOME, "Ready for pickup", "ለመረከብ ዝግጁ"),
        TranslationItemSpec("home_row_waiting_rec", TranslationCategory.HOME, "Waiting at reception", "በእንግዳ መቀበያ የሚጠብቁ"),
        TranslationItemSpec("home_row_cooking", TranslationCategory.HOME, "Cooking", "በመበሰል ላይ"),
        TranslationItemSpec("home_row_ready_serve", TranslationCategory.HOME, "Ready to serve", "ለመቅረብ ዝግጁ"),
        TranslationItemSpec("home_row_served_today", TranslationCategory.HOME, "Served today", "ዛሬ የቀረቡ"),
        TranslationItemSpec("home_row_received", TranslationCategory.HOME, "Received", "ተቀብሏል"),
        TranslationItemSpec("home_row_ready", TranslationCategory.HOME, "Ready", "ዝግጁ"),
        TranslationItemSpec("home_row_completed_today", TranslationCategory.HOME, "Completed today", "ዛሬ የተጠናቀቁ"),
        TranslationItemSpec("home_row_open_orders", TranslationCategory.HOME, "Open orders", "ክፍት ትዕዛዞች"),
        TranslationItemSpec("home_row_active", TranslationCategory.HOME, "Active", "ንቁ"),
        TranslationItemSpec("home_row_ready_go", TranslationCategory.HOME, "Ready to go", "ለመሄድ ዝግጁ"),
        TranslationItemSpec("home_action_order_food", TranslationCategory.HOME, "Order food", "ምግብ ይዘዙ"),
        TranslationItemSpec("home_action_kitchen_board", TranslationCategory.HOME, "Kitchen board", "የወጥ ቤት ሰሌዳ"),
        TranslationItemSpec("home_action_open_orders", TranslationCategory.HOME, "Open orders", "ትዕዛዞችን ክፈት"),
        TranslationItemSpec("home_completed_today_multiline", TranslationCategory.HOME, "Completed\ntoday", "ዛሬ\nየተጠናቀቁ"),
        TranslationItemSpec("home_last_7_days", TranslationCategory.HOME, "Last 7 days", "ያለፉት 7 ቀናት"),
        TranslationItemSpec("home_served_ring_sub", TranslationCategory.HOME, "served", "ቀርቧል"),
        TranslationItemSpec("home_latest_orders", TranslationCategory.HOME, "Latest orders", "የቅርብ ጊዜ ትዕዛዞች"),
        TranslationItemSpec("home_see_all", TranslationCategory.HOME, "See all", "ሁሉንም ይመልከቱ"),
        TranslationItemSpec("home_connecting_orders", TranslationCategory.HOME, "Connecting to live orders…", "ከቀጥታ ትዕዛዞች ጋር በመገናኘት ላይ…"),
        TranslationItemSpec("home_no_orders", TranslationCategory.HOME, "No orders yet.", "ገና ምንም ትዕዛዝ የለም።"),
        TranslationItemSpec("home_of_cap", TranslationCategory.HOME, " of {cap}", " ከ {cap}"),
        TranslationItemSpec("home_rooms_in_use", TranslationCategory.HOME, "rooms in use", "በአገልግሎት ላይ ያሉ ክፍሎች"),
        TranslationItemSpec("home_tables_in_use", TranslationCategory.HOME, "tables in use", "በአገልግሎት ላይ ያሉ ጠረጴዛዎች"),
        TranslationItemSpec("home_n_active", TranslationCategory.HOME, "{n} active", "{n} ንቁ"),
        TranslationItemSpec("floor_tap_room", TranslationCategory.HOME, "Tap a room to start an order", "ትዕዛዝ ለመጀመር ክፍል ይንኩ"),
        TranslationItemSpec("floor_tap_table", TranslationCategory.HOME, "Tap a table to start an order", "ትዕዛዝ ለመጀመር ጠረጴዛ ይንኩ"),
        TranslationItemSpec("floor_sub_hotel", TranslationCategory.HOME, "rooms in service", "በአገልግሎት ላይ ያሉ ክፍሎች"),
        TranslationItemSpec("floor_sub_dine", TranslationCategory.HOME, "tables in use", "በአገልግሎት ላይ ያሉ ጠረጴዛዎች"),
        TranslationItemSpec("floor_sub_rec", TranslationCategory.HOME, "desk orders", "የእንግዳ መቀበያ ትዕዛዞች"),
        TranslationItemSpec("floor_legend_free", TranslationCategory.HOME, "Free", "ነፃ"),
        TranslationItemSpec("floor_legend_new", TranslationCategory.HOME, "New", "አዲስ"),
        TranslationItemSpec("floor_legend_cooking", TranslationCategory.HOME, "Cooking", "በመበሰል ላይ"),
        TranslationItemSpec("floor_legend_ready", TranslationCategory.HOME, "Ready", "ዝግጁ"),
        TranslationItemSpec("floor_first_floor", TranslationCategory.HOME, "First floor · 101–116", "አንደኛ ፎቅ · 101–116"),
        TranslationItemSpec("floor_second_floor", TranslationCategory.HOME, "Second floor · 201–216", "ሁለተኛ ፎቅ · 201–216"),
        TranslationItemSpec("floor_stairs", TranslationCategory.HOME, "Stairs", "ደረጃ"),
        TranslationItemSpec("floor_lift", TranslationCategory.HOME, "Lift", "ሊፍት"),
        TranslationItemSpec("floor_corridor", TranslationCategory.HOME, "Corridor", "ኮሪደር"),
        TranslationItemSpec("floor_indoor", TranslationCategory.HOME, "Indoor Area", "የውስጥ መመገቢያ"),
        TranslationItemSpec("floor_lobby_lounge", TranslationCategory.HOME, "Lobby lounge", "ሎቢ ላውንጅ"),
        TranslationItemSpec("floor_patio", TranslationCategory.HOME, "Patio", "በረንዳ"),
        TranslationItemSpec("floor_vip", TranslationCategory.HOME, "VIP Area", "ቪአይፒ ክፍል"),
        TranslationItemSpec("floor_entrance", TranslationCategory.HOME, "Entrance", "መግቢያ"),
        TranslationItemSpec("floor_front_desk", TranslationCategory.HOME, "Front desk", "እንግዳ መቀበያ"),
        TranslationItemSpec("zone_rooms", TranslationCategory.HOME, "Rooms", "ክፍሎች"),
        TranslationItemSpec("zone_restaurant", TranslationCategory.HOME, "Restaurant", "ሬስቶራንት"),
        TranslationItemSpec("zone_reception", TranslationCategory.HOME, "Reception", "እንግዳ መቀበያ"),
        TranslationItemSpec("floor_room_prefix", TranslationCategory.HOME, "Room {loc}", "ክፍል {loc}"),
        TranslationItemSpec("place_selected_for", TranslationCategory.HOME, "Selected place", "የተመረጠ ቦታ"),
        TranslationItemSpec("place_change", TranslationCategory.HOME, "Change", "ቀይር"),

        // Menu & Saved
        TranslationItemSpec("menu_pill", TranslationCategory.MENU, "Menu", "ምናሌ"),
        TranslationItemSpec("menu_unit_dishes", TranslationCategory.MENU, "dishes", "ምግቦች"),
        TranslationItemSpec("menu_subtitle", TranslationCategory.MENU, "freshly prepared at Lakemark", "በሌክማርክ ትኩስ ተዘጋጅቶ የሚቀርብ"),
        TranslationItemSpec("menu_choose_spot", TranslationCategory.MENU, "Choose a room or table to begin", "ለመጀመር ክፍል ወይም ጠረጴዛ ይምረጡ"),
        TranslationItemSpec("menu_search_hint", TranslationCategory.MENU, "Search food or drink…", "ምግብ ወይም መጠጥ ይፈልጉ…"),
        TranslationItemSpec("menu_loading", TranslationCategory.MENU, "Loading menu…", "ምናሌ በመጫን ላይ…"),
        TranslationItemSpec("menu_empty", TranslationCategory.MENU, "No dishes match.", "ተዛማጅ ምግብ አልተገኘም።"),
        TranslationItemSpec("menu_add", TranslationCategory.MENU, "+ Add", "+ ጨምር"),
        TranslationItemSpec("menu_view_order", TranslationCategory.MENU, "View order · {count} · {total}", "ትዕዛዝ ይመልከቱ · {count} · {total}"),
        TranslationItemSpec("menu_save_item", TranslationCategory.MENU, "Save item", "ምግብ አስቀምጥ"),
        TranslationItemSpec("menu_remove_saved", TranslationCategory.MENU, "Remove saved", "ከተቀመጡ አስወግድ"),
        TranslationItemSpec("saved_title", TranslationCategory.MENU, "Saved Foods", "የተቀመጡ ምግቦች"),
        TranslationItemSpec("saved_pill", TranslationCategory.MENU, "Saved", "የተቀመጡ"),
        TranslationItemSpec("saved_unit", TranslationCategory.MENU, "saved", "የተቀመጡ"),
        TranslationItemSpec("saved_subtitle", TranslationCategory.MENU, "your bookmarked Lakemark dishes", "ለፈጣን ምርጫ ያስቀመጧቸው ምግቦች"),
        TranslationItemSpec("saved_empty_title", TranslationCategory.MENU, "No saved dishes yet", "እስካሁን የተቀመጡ ምግቦች የሉም"),
        TranslationItemSpec("saved_empty_hint", TranslationCategory.MENU, "Tap the bookmark icon on any dish in Menu to save it here for quick ordering.", "በፈጣን ለማዘዝ በምናሌ ውስጥ ያለውን የምልክት ማድረጊያ አዶ ይንኩ።"),
        TranslationItemSpec("saved_open_menu_btn", TranslationCategory.MENU, "Browse Menu", "ምናሌን ያስሱ"),
        TranslationItemSpec("saved_unavailable", TranslationCategory.MENU, "Currently off menu", "በአሁኑ ጊዜ የለም"),
        TranslationItemSpec("cat_all", TranslationCategory.MENU, "All", "ሁሉም"),
        TranslationItemSpec("cat_saved", TranslationCategory.MENU, "Saved", "የተቀመጡ"),
        TranslationItemSpec("cat_classics", TranslationCategory.MENU, "Classics", "ባህላዊ"),
        TranslationItemSpec("cat_lake_tana", TranslationCategory.MENU, "Lake Tana", "የጣና ሐይቅ"),
        TranslationItemSpec("cat_grill", TranslationCategory.MENU, "Grill", "ጥብስ እና ግሪል"),
        TranslationItemSpec("cat_breakfast", TranslationCategory.MENU, "Breakfast", "ቁርስ"),
        TranslationItemSpec("cat_drinks", TranslationCategory.MENU, "Drinks", "መጠጦች"),
        TranslationItemSpec("cart_title", TranslationCategory.MENU, "Your Order", "ትዕዛዝዎ"),
        TranslationItemSpec("cart_your_name", TranslationCategory.MENU, "Guest / table name", "የእንግዳ / ጠረጴዛ ስም"),
        TranslationItemSpec("cart_special_note", TranslationCategory.MENU, "Special kitchen note (optional)", "ለወጥ ቤት ልዩ ማስታወሻ (አማራጭ)"),
        TranslationItemSpec("cart_subtotal", TranslationCategory.MENU, "Subtotal", "ንዑስ ድምር"),
        TranslationItemSpec("cart_vat", TranslationCategory.MENU, "VAT (15%)", "ተጨማሪ እሴት ታክስ (15%)"),
        TranslationItemSpec("cart_total", TranslationCategory.MENU, "Total", "ጠቅላላ ድምር"),
        TranslationItemSpec("cart_place_order", TranslationCategory.MENU, "Send order", "ትዕዛዝ ላክ"),
        TranslationItemSpec("cart_takeaway_phone", TranslationCategory.MENU, "Phone / takeaway label", "ስልክ / የሚወሰድ መለያ"),
        TranslationItemSpec("cart_empty_spot", TranslationCategory.MENU, "Tap a room or table on Floor first", "መጀመሪያ በወለል ላይ ክፍል ወይም ጠረጴዛ ይምረጡ"),

        // Default Menu Dishes
        TranslationItemSpec("dish_name_m0", TranslationCategory.MENU, "Doro Wat", "ዶሮ ወጥ"),
        TranslationItemSpec("dish_desc_m0", TranslationCategory.MENU, "Chicken in berbere, egg, injera", "በበርበሬ የተሰራ ዶሮ ወጥ፣ እንቁላል እና እንጀራ"),
        TranslationItemSpec("dish_name_m1", TranslationCategory.MENU, "Kitfo", "ክትፎ"),
        TranslationItemSpec("dish_desc_m1", TranslationCategory.MENU, "Minced beef, mitmita, niter kibbeh", "የተከተፈ ስጋ፣ ሚጥሚጣ እና ንጥር ቅቤ"),
        TranslationItemSpec("dish_name_m2", TranslationCategory.MENU, "Siga Tibs", "ስጋ ጥብስ"),
        TranslationItemSpec("dish_desc_m2", TranslationCategory.MENU, "Sautéed beef, onion, rosemary", "የበሬ ስጋ ጥብስ፣ ሽንኩርት እና ሮዝሜሪ"),
        TranslationItemSpec("dish_name_m3", TranslationCategory.MENU, "Shiro Wat", "ሽሮ ወጥ"),
        TranslationItemSpec("dish_desc_m3", TranslationCategory.MENU, "Chickpea stew, injera (fasting)", "የሽምብራ ሽሮ ወጥ እና እንጀራ (የጾም)"),
        TranslationItemSpec("dish_name_m4", TranslationCategory.MENU, "Fish Fillet", "የዓሳ ፊሌት"),
        TranslationItemSpec("dish_desc_m4", TranslationCategory.MENU, "Fresh Lake Tana fillet, lemon, garlic", "ትኩስ የጣና ሐይቅ ዓሳ ፊሌት፣ ሎሚ እና ነጭ ሽንኩርት"),
        TranslationItemSpec("dish_name_m5", TranslationCategory.MENU, "Fish Cutlet", "የዓሳ ከትሌት"),
        TranslationItemSpec("dish_desc_m5", TranslationCategory.MENU, "Crisp cutlets, spiced tartare", "ጥርት ያለ የዓሳ ከትሌት እና ቅመም ያለው ሶስ"),
        TranslationItemSpec("dish_name_m6", TranslationCategory.MENU, "Club Sandwich", "ክለብ ሳንድዊች"),
        TranslationItemSpec("dish_desc_m6", TranslationCategory.MENU, "Chicken, egg, tomato, chips", "ዶሮ፣ እንቁላል፣ ቲማቲም እና ችፕስ"),
        TranslationItemSpec("dish_name_m7", TranslationCategory.MENU, "Beef Burger", "ቢፍ በርገር"),
        TranslationItemSpec("dish_desc_m7", TranslationCategory.MENU, "Patty, cheese, house sauce, chips", "ስጋ፣ አይብ፣ ልዩ ሶስ እና ችፕስ"),
        TranslationItemSpec("dish_name_m8", TranslationCategory.MENU, "Fuul", "ፉል"),
        TranslationItemSpec("dish_desc_m8", TranslationCategory.MENU, "Fava beans, tomato, chilli, bread", "ባቄላ ፉል፣ ቲማቲም፣ ቃሪያ እና ዳቦ"),
        TranslationItemSpec("dish_name_m9", TranslationCategory.MENU, "Firfir", "ፍርፍር"),
        TranslationItemSpec("dish_desc_m9", TranslationCategory.MENU, "Shredded injera, spiced sauce", "የተቆረጠ እንጀራ በቅመም ስጎ"),
        TranslationItemSpec("dish_name_m10", TranslationCategory.MENU, "Buna", "ቡና"),
        TranslationItemSpec("dish_desc_m10", TranslationCategory.MENU, "Ethiopian coffee in a jebena", "ባህላዊ የኢትዮጵያ ጀበና ቡና"),
        TranslationItemSpec("dish_name_m11", TranslationCategory.MENU, "Spris Juice", "ስፕሪስ ጁስ"),
        TranslationItemSpec("dish_desc_m11", TranslationCategory.MENU, "Avocado, mango, papaya", "አቮካዶ፣ ማንጎ እና ፓፓያ"),

        // Orders & Kitchen
        TranslationItemSpec("orders_filter_active", TranslationCategory.ORDERS, "Active", "ንቁ"),
        TranslationItemSpec("orders_filter_finished", TranslationCategory.ORDERS, "Finished", "የተጠናቀቁ"),
        TranslationItemSpec("orders_zone_all", TranslationCategory.ORDERS, "All", "ሁሉም"),
        TranslationItemSpec("orders_empty", TranslationCategory.ORDERS, "No orders here.", "እዚህ ምንም ትዕዛዝ የለም።"),
        TranslationItemSpec("orders_today", TranslationCategory.ORDERS, "Today", "ዛሬ"),
        TranslationItemSpec("orders_yesterday", TranslationCategory.ORDERS, "Yesterday", "ትናንት"),
        TranslationItemSpec("orders_assign_waiter", TranslationCategory.ORDERS, "Assign", "መድብ"),
        TranslationItemSpec("orders_assign_title", TranslationCategory.ORDERS, "Assign #{num}", "#{num}ን መድብ"),
        TranslationItemSpec("orders_auto_assign", TranslationCategory.ORDERS, "Auto-assign nearest", "በቅርብ ያለውን በራስ-ሰር መድብ"),
        TranslationItemSpec("orders_assigned_badge", TranslationCategory.ORDERS, "Assigned", "ተመድቧል"),
        TranslationItemSpec("orders_off_duty", TranslationCategory.ORDERS, "Off duty", "ከስራ ውጭ"),
        TranslationItemSpec("orders_on_duty", TranslationCategory.ORDERS, "On duty", "በስራ ላይ"),
        TranslationItemSpec("orders_no_area", TranslationCategory.ORDERS, "No area", "ቦታ የለም"),
        TranslationItemSpec("orders_active_load", TranslationCategory.ORDERS, "{n} active", "{n} ንቁ"),
        TranslationItemSpec("kitchen_stage_received", TranslationCategory.ORDERS, "Received", "ተቀብሏል"),
        TranslationItemSpec("kitchen_stage_received_sub", TranslationCategory.ORDERS, "new tickets", "አዲስ ትኬቶች"),
        TranslationItemSpec("kitchen_stage_preparing", TranslationCategory.ORDERS, "Preparing", "በዝግጅት ላይ"),
        TranslationItemSpec("kitchen_stage_preparing_sub", TranslationCategory.ORDERS, "on the stove", "በምድጃ ላይ"),
        TranslationItemSpec("kitchen_stage_ready", TranslationCategory.ORDERS, "Ready", "ዝግጁ"),
        TranslationItemSpec("kitchen_stage_ready_sub", TranslationCategory.ORDERS, "to serve", "ለመቅረብ"),
        TranslationItemSpec("kitchen_nothing_here", TranslationCategory.ORDERS, "Nothing here.", "እዚህ ምንም የለም።"),
        TranslationItemSpec("dialog_new_order_header", TranslationCategory.ORDERS, "New order · {brand}", "አዲስ ትዕዛዝ · {brand}"),
        TranslationItemSpec("dialog_order_ready_header", TranslationCategory.ORDERS, "Order ready · {brand}", "ትዕዛዝ ዝግጁ ነው · {brand}"),
        TranslationItemSpec("dialog_waiting_more", TranslationCategory.ORDERS, "+{n} more waiting", "+{n} ተጨማሪ በመጠበቅ ላይ"),
        TranslationItemSpec("dialog_ready_more", TranslationCategory.ORDERS, "+{n} more ready", "+{n} ተጨማሪ ዝግጁ"),
        TranslationItemSpec("dialog_note_prefix", TranslationCategory.ORDERS, "Note: {note}", "ማስታወሻ: {note}"),
        TranslationItemSpec("dialog_accept", TranslationCategory.ORDERS, "Accept", "ተቀበል"),
        TranslationItemSpec("dialog_cancel_reason_prompt", TranslationCategory.ORDERS, "Choose or type a reason.", "ምክንያት ይምረጡ ወይም ይጻፉ።"),
        TranslationItemSpec("dialog_cancel_reason_hint", TranslationCategory.ORDERS, "Reason for cancellation", "የመሰረዝ ምክንያት"),
        TranslationItemSpec("cancel_reason_out_of_stock", TranslationCategory.ORDERS, "Out of stock", "አልቋል"),
        TranslationItemSpec("cancel_reason_kitchen_busy", TranslationCategory.ORDERS, "Kitchen at capacity", "ወጥ ቤት በጣም ተጨናንቋል"),
        TranslationItemSpec("cancel_reason_guest_cancelled", TranslationCategory.ORDERS, "Guest cancelled", "እንግዳ ሰርዟል"),

        // Manage & Team
        TranslationItemSpec("manage_pill", TranslationCategory.MANAGE, "Manage", "አስተዳድር"),
        TranslationItemSpec("manage_hero_unit", TranslationCategory.MANAGE, "orders", "ትዕዛዞች"),
        TranslationItemSpec("manage_hero_sub", TranslationCategory.MANAGE, "recorded across Lakemark", "በሌክማርክ የተመዘገቡ"),
        TranslationItemSpec("manage_tab_overview", TranslationCategory.MANAGE, "Overview", "አጠቃላይ"),
        TranslationItemSpec("manage_tab_menu", TranslationCategory.MANAGE, "Menu", "ምናሌ"),
        TranslationItemSpec("manage_tab_team", TranslationCategory.MANAGE, "Team", "ቡድን"),
        TranslationItemSpec("manage_tab_language", TranslationCategory.MANAGE, "Language", "ቋንቋ"),
        TranslationItemSpec("manage_tab_alerts", TranslationCategory.MANAGE, "Alerts", "ማንቂያዎች"),
        TranslationItemSpec("team_sub_accounts", TranslationCategory.MANAGE, "Accounts", "መለያዎች"),
        TranslationItemSpec("team_sub_new_registered", TranslationCategory.MANAGE, "New Users", "አዲስ ተጠቃሚዎች"),
        TranslationItemSpec("team_sub_waiters", TranslationCategory.MANAGE, "Waiters", "አስተናጋጆች"),
        TranslationItemSpec("team_sub_brands", TranslationCategory.MANAGE, "Brands", "ብራንዶች"),
        TranslationItemSpec("manage_kpi_today", TranslationCategory.MANAGE, "Today", "ዛሬ"),
        TranslationItemSpec("manage_kpi_today_sub", TranslationCategory.MANAGE, "{n} done · {open} open", "{n} የተጠናቀቁ · {open} ክፍት"),
        TranslationItemSpec("manage_kpi_sales", TranslationCategory.MANAGE, "Today sales", "የዛሬ ሽያጭ"),
        TranslationItemSpec("manage_kpi_sales_sub", TranslationCategory.MANAGE, "all non-cancelled orders", "ያልተሰረዙ ትዕዛዞች በሙሉ"),
        TranslationItemSpec("manage_kpi_waiters", TranslationCategory.MANAGE, "Waiters live", "ንቁ አስተናጋጆች"),
        TranslationItemSpec("manage_kpi_waiters_sub", TranslationCategory.MANAGE, "of {total} waiter accounts", "ከ {total} የአስተናጋጅ መለያዎች"),
        TranslationItemSpec("manage_kpi_menu", TranslationCategory.MANAGE, "Menu live", "ንቁ ምናሌ"),
        TranslationItemSpec("manage_kpi_menu_sub", TranslationCategory.MANAGE, "of {total} dishes active", "ከ {total} ምግቦች ንቁ"),
        TranslationItemSpec("manage_reports_title", TranslationCategory.MANAGE, "Reports & analytics", "ሪፖርቶች እና ትንተና"),
        TranslationItemSpec("manage_top_dishes", TranslationCategory.MANAGE, "Top dishes", "ተወዳጅ ምግቦች"),
        TranslationItemSpec("manage_menu_price_etb", TranslationCategory.MANAGE, "ETB", "ብር"),
        TranslationItemSpec("manage_menu_photo_add", TranslationCategory.MANAGE, "Photo", "ፎቶ"),
        TranslationItemSpec("manage_menu_photo_replace", TranslationCategory.MANAGE, "Replace photo", "ፎቶ ቀይር"),
        TranslationItemSpec("manage_menu_photo_remove", TranslationCategory.MANAGE, "Remove photo", "ፎቶ አስወግድ"),
        TranslationItemSpec("manage_menu_view_saved", TranslationCategory.MANAGE, "Saved ({n})", "የተቀመጡ ({n})"),
        TranslationItemSpec("manage_new_reg_title", TranslationCategory.MANAGE, "Newly registered users", "አዲስ የተመዘገቡ ተጠቃሚዎች"),
        TranslationItemSpec(
            "manage_new_reg_desc",
            TranslationCategory.MANAGE,
            "Recent sign-ups, registration status, and Manager activation controls.",
            "የቅርብ ጊዜ ምዝገባዎች፣ የምዝገባ ሁኔታ እና የአስተዳዳሪ ማጽደቂያ።",
        ),
        TranslationItemSpec("manage_reg_status_pending", TranslationCategory.MANAGE, "Pending approval", "ማጽደቅ በመጠባበቅ ላይ"),
        TranslationItemSpec("manage_reg_status_perm", TranslationCategory.MANAGE, "Permanently activated", "በቋሚነት የነቃ"),
        TranslationItemSpec("manage_reg_status_active", TranslationCategory.MANAGE, "Active", "ንቁ"),
        TranslationItemSpec("manage_reg_status_disabled", TranslationCategory.MANAGE, "Disabled", "ተዘግቷል"),
        TranslationItemSpec("manage_reg_at", TranslationCategory.MANAGE, "Registered: {time}", "የተመዘገበበት: {time}"),
        TranslationItemSpec("manage_approve_btn", TranslationCategory.MANAGE, "Approve", "አጽድቅ"),
        TranslationItemSpec("manage_approved_perm_badge", TranslationCategory.MANAGE, "Permanent", "ቋሚ"),
        TranslationItemSpec("manage_create_staff_title", TranslationCategory.MANAGE, "Create staff account", "የሰራተኛ መለያ ፍጠር"),
        TranslationItemSpec("manage_create_staff_name", TranslationCategory.MANAGE, "Full name", "ሙሉ ስም"),
        TranslationItemSpec("manage_create_staff_user", TranslationCategory.MANAGE, "Username", "የተጠቃሚ ስም"),
        TranslationItemSpec("manage_create_staff_pass", TranslationCategory.MANAGE, "Initial password", "የመጀመሪያ ይለፍ ቃል"),
        TranslationItemSpec("manage_create_staff_btn", TranslationCategory.MANAGE, "Create staff account", "የሰራተኛ መለያ ፍጠር"),
        TranslationItemSpec("manage_reset_pw_btn", TranslationCategory.MANAGE, "Reset password", "የይለፍ ቃል ዳግም አስጀምር"),
        TranslationItemSpec("manage_edit_staff_btn", TranslationCategory.MANAGE, "Edit", "አርትዕ"),
        TranslationItemSpec("manage_edit_staff_title", TranslationCategory.MANAGE, "Edit staff account", "የሰራተኛ መለያ አርትዕ"),
        TranslationItemSpec("manage_new_password_optional", TranslationCategory.MANAGE, "New password (optional)", "አዲስ የይለፍ ቃል (አማራጭ)"),
        TranslationItemSpec("manage_account_active_label", TranslationCategory.MANAGE, "Account active", "መለያ ንቁ ነው"),
        TranslationItemSpec("manage_lang_title", TranslationCategory.MANAGE, "Language Management", "የቋንቋ አስተዳደር"),
        TranslationItemSpec(
            "manage_lang_subtitle",
            TranslationCategory.MANAGE,
            "Edit Amharic & English translations synced live across all users.",
            "ለሁሉም ተጠቃሚዎች በቀጥታ የሚሰራጩ የአማርኛ እና የእንግሊዝኛ ትርጉሞችን ያርትዑ።",
        ),
        TranslationItemSpec("manage_lang_search", TranslationCategory.MANAGE, "Search English, Amharic, or key…", "በእንግሊዝኛ፣ በአማርኛ ወይም በቁልፍ ይፈልጉ…"),
        TranslationItemSpec("manage_lang_edit_title", TranslationCategory.MANAGE, "Edit translation", "ትርጉም ያርትዑ"),
        TranslationItemSpec("manage_lang_en_label", TranslationCategory.MANAGE, "English source", "የእንግሊዝኛ ምንጭ"),
        TranslationItemSpec("manage_lang_am_label", TranslationCategory.MANAGE, "Amharic translation", "የአማርኛ ትርጉም"),
        TranslationItemSpec("manage_lang_restore_default", TranslationCategory.MANAGE, "Restore default", "ነባሪውን መልስ"),
        TranslationItemSpec("manage_lang_no_matches", TranslationCategory.MANAGE, "No translation keys match your filters.", "ከማጣሪያዎ ጋር የሚዛመድ ቁልፍ የለም።"),
        TranslationItemSpec("lang_status_all", TranslationCategory.MANAGE, "All", "ሁሉም"),
        TranslationItemSpec("lang_status_translated", TranslationCategory.MANAGE, "Translated", "ተተርጉሟል"),
        TranslationItemSpec("lang_status_untranslated", TranslationCategory.MANAGE, "Untranslated", "ያልተተረጎመ"),
        TranslationItemSpec("lang_status_outdated", TranslationCategory.MANAGE, "Outdated", "ጊዜው ያለፈበት"),
        TranslationItemSpec("lang_status_missing", TranslationCategory.MANAGE, "Missing", "የጎደለ"),
        TranslationItemSpec("manage_alerts_send_title", TranslationCategory.MANAGE, "Send to staff", "ለሰራተኞች ላክ"),
        TranslationItemSpec("manage_alerts_everyone", TranslationCategory.MANAGE, "Everyone", "ለሁሉም"),
        TranslationItemSpec("manage_alerts_to_prefix", TranslationCategory.MANAGE, "To: {target}", "ለ: {target}"),
        TranslationItemSpec("manage_alerts_hint", TranslationCategory.MANAGE, "Write a message for {target}", "ለ{target} መልእክት ይጻፉ"),
        TranslationItemSpec("manage_alerts_send_btn", TranslationCategory.MANAGE, "Send notification", "ማሳወቂያ ላክ"),
        TranslationItemSpec("manage_alerts_activity", TranslationCategory.MANAGE, "Operational activity", "የስራ እንቅስቃሴ"),
        TranslationItemSpec("manage_release_title", TranslationCategory.MANAGE, "Release Update & Delivery", "የመተግበሪያ ማሻሻያ ማሰራጫ"),
        TranslationItemSpec("manage_release_desc", TranslationCategory.MANAGE, "Notify active users when a new APK release is available.", "አዲስ የAPK ማሻሻያ ሲኖር ለንቁ ተጠቃሚዎች ያሳውቁ።"),
        TranslationItemSpec("manage_release_version_name", TranslationCategory.MANAGE, "Version name", "የስሪት ስም"),
        TranslationItemSpec("manage_release_version_code", TranslationCategory.MANAGE, "Version code", "የስሪት ቁጥር"),
        TranslationItemSpec("manage_release_apk_url", TranslationCategory.MANAGE, "APK download URL", "የAPK ማውረጃ አድራሻ"),
        TranslationItemSpec("manage_release_sha256", TranslationCategory.MANAGE, "APK SHA-256 (optional)", "የAPK SHA-256 ማረጋገጫ (አማራጭ)"),
        TranslationItemSpec("manage_release_notes", TranslationCategory.MANAGE, "Release notes", "የማሻሻያ ማስታወሻ"),
        TranslationItemSpec("manage_release_mandatory", TranslationCategory.MANAGE, "Mandatory update prompt", "ግዴታ የሆነ ማሻሻያ"),
        TranslationItemSpec("manage_release_publish_btn", TranslationCategory.MANAGE, "Publish & notify users", "አትም እና ለተጠቃሚዎች አሳውቅ"),

        // Profile & Settings
        TranslationItemSpec("settings_title", TranslationCategory.PROFILE, "Profile & Settings", "መገለጫ እና ቅንብሮች"),
        TranslationItemSpec("profile_photo_title", TranslationCategory.PROFILE, "Profile Photo", "የመገለጫ ፎቶ"),
        TranslationItemSpec("profile_photo_change", TranslationCategory.PROFILE, "Change photo", "ፎቶ ቀይር"),
        TranslationItemSpec("profile_photo_add", TranslationCategory.PROFILE, "Add photo", "ፎቶ ጨምር"),
        TranslationItemSpec("profile_photo_remove", TranslationCategory.PROFILE, "Remove photo", "ፎቶ አስወግድ"),
        TranslationItemSpec("profile_name_label", TranslationCategory.PROFILE, "Full name", "ሙሉ ስም"),
        TranslationItemSpec("profile_name_save", TranslationCategory.PROFILE, "Update profile", "መገለጫ አዘምን"),
        TranslationItemSpec("profile_assigned_role", TranslationCategory.PROFILE, "Assigned role: {role}", "የተመደበ ሚና: {role}"),
        TranslationItemSpec("profile_preview_badge", TranslationCategory.PROFILE, "Previewing: {role}", "ቅድመ እይታ: {role}"),
        TranslationItemSpec("settings_language_title", TranslationCategory.PROFILE, "Language / ቋንቋ", "ቋንቋ / Language"),
        TranslationItemSpec("settings_language_sub", TranslationCategory.PROFILE, "Applies immediately across the app", "ወዲያውኑ በመተግበሪያው ውስጥ ይተገበራል"),
        TranslationItemSpec("settings_workspace_title", TranslationCategory.PROFILE, "Role workspace preview", "የስራ ክፍል ቅድመ እይታ"),
        TranslationItemSpec("settings_workspace_note", TranslationCategory.PROFILE, "Your assigned designation remains Manager.", "የተመደበው ማዕረግዎ አስተዳዳሪ እንደሆነ ይቆያል።"),
        TranslationItemSpec("settings_saved_shortcut", TranslationCategory.PROFILE, "Saved dishes ({n})", "የተቀመጡ ምግቦች ({n})"),
        TranslationItemSpec("settings_sound_title", TranslationCategory.PROFILE, "Alert sound", "የማንቂያ ድምጽ"),
        TranslationItemSpec("settings_vibration_title", TranslationCategory.PROFILE, "Vibration", "ንዝረት"),
        TranslationItemSpec("settings_volume_title", TranslationCategory.PROFILE, "Alert volume", "የድምጽ መጠን"),
        TranslationItemSpec("settings_notif_perm_title", TranslationCategory.PROFILE, "System notifications", "የስርዓት ማሳወቂያዎች"),
        TranslationItemSpec("settings_notif_enabled", TranslationCategory.PROFILE, "Enabled", "ነቅቷል"),
        TranslationItemSpec("settings_notif_enable_btn", TranslationCategory.PROFILE, "Enable", "አንቃ"),
        TranslationItemSpec("settings_test_sound", TranslationCategory.PROFILE, "Test sound", "ድምጽ ሞክር"),
        TranslationItemSpec("settings_notif_open_sys", TranslationCategory.PROFILE, "App settings", "የመተግበሪያ ቅንብሮች"),
        TranslationItemSpec("settings_notif_channels_ok", TranslationCategory.PROFILE, "All alert channels active with sound", "ሁሉም የማንቂያ ቻናሎች ከድምጽ ጋር ነቅተዋል"),
        TranslationItemSpec("settings_notif_channels_muted", TranslationCategory.PROFILE, "Channel sound muted in Android settings", "የቻናል ድምጽ በAndroid ቅንብር ዝግ ነው"),
        TranslationItemSpec("settings_device_vol_muted", TranslationCategory.PROFILE, "Phone media volume is muted (0%) — raise device volume to hear alerts.", "የስልክ ሚዲያ ድምጽ ዝግ ነው (0%) — ማንቂያ ለመስማት የስልክ ድምጽ ይጨምሩ።"),
        TranslationItemSpec("val_test_sound_played", TranslationCategory.VALIDATION, "Alert chime & vibration tested.", "የማንቂያ ድምጽ እና ንዝረት ተሞክሯል።"),
        TranslationItemSpec("settings_my_brand", TranslationCategory.PROFILE, "Customize role theme", "የሚና ገጽታ ያስተካክሉ"),
        TranslationItemSpec("media_picker_title", TranslationCategory.PROFILE, "Choose image source", "የምስል ምንጭ ይምረጡ"),
        TranslationItemSpec("media_picker_gallery", TranslationCategory.PROFILE, "Choose from gallery", "ከጋለሪ ይምረጡ"),
        TranslationItemSpec("media_picker_camera", TranslationCategory.PROFILE, "Take a photo", "ፎቶ አንሳ"),
        TranslationItemSpec("media_uploading", TranslationCategory.PROFILE, "Uploading image…", "ምስል በመጫን ላይ…"),
        TranslationItemSpec("notif_sheet_title", TranslationCategory.PROFILE, "Notifications", "ማሳወቂያዎች"),
        TranslationItemSpec("notif_empty", TranslationCategory.PROFILE, "No notifications yet.", "ገና ምንም ማሳወቂያ የለም።"),
        TranslationItemSpec("notif_update_available", TranslationCategory.PROFILE, "App update available", "የመተግበሪያ ማሻሻያ አለ"),
        TranslationItemSpec("notif_update_btn", TranslationCategory.PROFILE, "Update Now", "አሁን አዘምን"),
        TranslationItemSpec("notif_update_now", TranslationCategory.PROFILE, "Update Now", "አሁን አዘምን"),
        TranslationItemSpec("notif_update_dialog_title", TranslationCategory.PROFILE, "Update Available · v{ver}", "ማሻሻያ አለ · v{ver}"),
        TranslationItemSpec("notif_update_open_link", TranslationCategory.PROFILE, "Update Now", "አሁን አዘምን"),
        TranslationItemSpec("notif_update_downloading", TranslationCategory.PROFILE, "Downloading verified APK…", "የተረጋገጠ APK በማውረድ ላይ…"),
        TranslationItemSpec("notif_update_verifying", TranslationCategory.PROFILE, "Verifying APK integrity & signature…", "የAPK ትክክለኛነት እና ፊርማ በማረጋገጥ ላይ…"),
        TranslationItemSpec("notif_update_install_ready", TranslationCategory.PROFILE, "Verified · Opening Android installer", "ተረጋግጧል · የAndroid መጫኛ በመክፈት ላይ"),
        TranslationItemSpec("notif_update_allow_install", TranslationCategory.PROFILE, "Allow app installs", "መተግበሪያ መጫን ፍቀድ"),
        TranslationItemSpec("notif_update_open_browser", TranslationCategory.PROFILE, "Open download page", "የማውረጃ ገጽ ክፈት"),

        // Roles & Statuses
        TranslationItemSpec("role_manager", TranslationCategory.STATUS, "Manager", "አስተዳዳሪ"),
        TranslationItemSpec("role_reception", TranslationCategory.STATUS, "Receptionist", "እንግዳ ተቀባይ"),
        TranslationItemSpec("role_kitchen", TranslationCategory.STATUS, "Kitchen Staff", "የወጥ ቤት ሰራተኛ"),
        TranslationItemSpec("role_waiter", TranslationCategory.STATUS, "Waiter", "አስተናጋጅ"),
        TranslationItemSpec("role_cashier", TranslationCategory.STATUS, "Cashier", "ገንዘብ ተቀባይ"),
        TranslationItemSpec("role_delivery", TranslationCategory.STATUS, "Delivery", "አድራሽ"),
        TranslationItemSpec("role_customer", TranslationCategory.STATUS, "Customer", "ደንበኛ"),
        TranslationItemSpec("status_new", TranslationCategory.STATUS, "At reception", "በእንግዳ መቀበያ"),
        TranslationItemSpec("status_new_short", TranslationCategory.STATUS, "New", "አዲስ"),
        TranslationItemSpec("status_confirmed", TranslationCategory.STATUS, "Received by kitchen", "ወጥ ቤት ደርሷል"),
        TranslationItemSpec("status_preparing", TranslationCategory.STATUS, "Preparing", "በመበሰል ላይ"),
        TranslationItemSpec("status_ready", TranslationCategory.STATUS, "Ready", "ዝግጁ"),
        TranslationItemSpec("status_done", TranslationCategory.STATUS, "Completed", "ተጠናቋል"),
        TranslationItemSpec("status_cancelled", TranslationCategory.STATUS, "Cancelled", "ተሰርዟል"),
        TranslationItemSpec("action_send_to_kitchen", TranslationCategory.STATUS, "Send to kitchen", "ወደ ወጥ ቤት ላክ"),
        TranslationItemSpec("action_start_preparing", TranslationCategory.STATUS, "Start preparing", "ዝግጅት ጀምር"),
        TranslationItemSpec("action_mark_ready", TranslationCategory.STATUS, "Mark ready", "ዝግጁ አድርግ"),
        TranslationItemSpec("action_confirm_pickup", TranslationCategory.STATUS, "Confirm pickup", "ርክክብ አረጋግጥ"),
        TranslationItemSpec("action_cancel", TranslationCategory.STATUS, "Cancel", "ሰርዝ"),
        TranslationItemSpec("order_type_room", TranslationCategory.STATUS, "Room service", "የክፍል አገልግሎት"),
        TranslationItemSpec("order_type_dine", TranslationCategory.STATUS, "Dine-in", "በሬስቶራንት"),
        TranslationItemSpec("order_type_pickup", TranslationCategory.STATUS, "Reception", "እንግዳ መቀበያ"),
        TranslationItemSpec("order_type_out", TranslationCategory.STATUS, "Takeaway / delivery", "የሚወሰድ / አድራሻ"),
        TranslationItemSpec("prompt_room_number", TranslationCategory.STATUS, "Room number", "የክፍል ቁጥር"),
        TranslationItemSpec("prompt_table_number", TranslationCategory.STATUS, "Table number", "የጠረጴዛ ቁጥር"),
        TranslationItemSpec("prompt_reception_table", TranslationCategory.STATUS, "Reception table", "የእንግዳ መቀበያ ጠረጴዛ"),
        TranslationItemSpec("prompt_phone_number", TranslationCategory.STATUS, "Phone number", "ስልክ ቁጥር"),
        TranslationItemSpec("area_restaurant", TranslationCategory.STATUS, "Restaurant", "ሬስቶራንት"),
        TranslationItemSpec("area_lobby", TranslationCategory.STATUS, "Lobby", "ሎቢ"),
        TranslationItemSpec("area_floor_1", TranslationCategory.STATUS, "Floor 1", "ፎቅ 1"),
        TranslationItemSpec("area_floor_2", TranslationCategory.STATUS, "Floor 2", "ፎቅ 2"),

        // Alerts & Validation
        TranslationItemSpec("notif_order_new", TranslationCategory.VALIDATION, "New order #{num} placed · {loc}", "አዲስ ትዕዛዝ #{num} ገብቷል · {loc}"),
        TranslationItemSpec("notif_order_confirmed", TranslationCategory.VALIDATION, "Order #{num} received by the kitchen · {loc}", "ትዕዛዝ #{num} ወጥ ቤት ደርሷል · {loc}"),
        TranslationItemSpec("notif_order_preparing", TranslationCategory.VALIDATION, "Order #{num} is being prepared · {loc}", "ትዕዛዝ #{num} በመበሰል ላይ ነው · {loc}"),
        TranslationItemSpec("notif_order_ready", TranslationCategory.VALIDATION, "Order #{num} is ready · {loc}", "ትዕዛዝ #{num} ዝግጁ ነው · {loc}"),
        TranslationItemSpec("notif_order_done", TranslationCategory.VALIDATION, "Order #{num} completed · {loc}", "ትዕዛዝ #{num} ተጠናቋል · {loc}"),
        TranslationItemSpec("notif_order_cancelled", TranslationCategory.VALIDATION, "Order #{num} cancelled · {loc}", "ትዕዛዝ #{num} ተሰርዟል · {loc}"),
        TranslationItemSpec("notif_order_cancelled_reason", TranslationCategory.VALIDATION, "Order #{num} cancelled by kitchen: {reason}", "ትዕዛዝ #{num} በወጥ ቤት ተሰርዟል: {reason}"),
        TranslationItemSpec("val_camera_denied", TranslationCategory.VALIDATION, "Camera permission is needed to take a photo.", "ፎቶ ለማንሳት የካሜራ ፍቃድ ያስፈልጋል።"),
        TranslationItemSpec("val_image_invalid_type", TranslationCategory.VALIDATION, "Unsupported image type. Please choose JPEG, PNG, or WebP.", "ያልተደገፈ የምስል ዓይነት። እባክዎ JPEG፣ PNG ወይም WebP ይምረጡ።"),
        TranslationItemSpec("val_image_too_large", TranslationCategory.VALIDATION, "Image is too large even after compression (max 5 MB).", "ምስሉ ከ5 ሜባ በላይ ስለሆነ መላክ አልተቻለም።"),
        TranslationItemSpec("val_image_upload_failed", TranslationCategory.VALIDATION, "Image upload failed: {err}", "ምስል መጫን አልተቻለም: {err}"),
        TranslationItemSpec("val_profile_updated", TranslationCategory.VALIDATION, "Profile updated.", "መገለጫው ተዘምኗል።"),
        TranslationItemSpec("val_food_image_updated", TranslationCategory.VALIDATION, "Dish image updated.", "የምግብ ፎቶ ተዘምኗል።"),
        TranslationItemSpec("val_translation_saved", TranslationCategory.VALIDATION, "Translation saved & synced.", "ትርጉሙ ተቀምጧል እና ተሰራጭቷል።"),
        TranslationItemSpec("val_saved_added", TranslationCategory.VALIDATION, "Saved to bookmarks.", "ወደ የተቀመጡ ተጨምሯል።"),
        TranslationItemSpec("val_saved_removed", TranslationCategory.VALIDATION, "Removed from bookmarks.", "ከተቀመጡ ተወግዷል።"),
        TranslationItemSpec("val_name_required", TranslationCategory.VALIDATION, "Please enter a full name.", "እባክዎ ሙሉ ስም ያስገቡ።"),
        TranslationItemSpec("val_release_published", TranslationCategory.VALIDATION, "Update notification sent.", "የማሻሻያ ማሳወቂያ ተልኳል።"),
    )

    private val defaultByKey: Map<String, TranslationItemSpec> = defaultEntries.associateBy { it.key }

    fun defaultEnglishStrings(): AppStrings = buildStrings(AppLanguage.EN, emptyMap())

    fun buildStrings(
        language: AppLanguage,
        remoteOverrides: Map<String, RemoteTranslation>,
    ): AppStrings {
        val resolved = LinkedHashMap<String, String>(defaultEntries.size + remoteOverrides.size)
        for (spec in defaultEntries) {
            val remote = remoteOverrides[spec.key]
            val enText = remote?.en?.trim()?.ifBlank { null } ?: spec.en
            val value = when (language) {
                AppLanguage.EN -> enText
                AppLanguage.AM -> remote?.am?.trim()?.ifBlank { null }
                    ?: spec.am.trim().ifBlank { null }
                    ?: enText
            }
            resolved[spec.key] = value
        }
        // Also merge any extra dynamic keys added by Managers remotely
        for ((key, remote) in remoteOverrides) {
            if (!resolved.containsKey(key)) {
                val enText = remote.en.trim()
                val value = if (language == AppLanguage.AM) {
                    remote.am.trim().ifBlank { enText }
                } else {
                    enText
                }
                if (value.isNotBlank()) {
                    resolved[key] = value
                }
            }
        }
        return AppStrings(language = language, resolved = resolved)
    }

    fun buildRecords(remoteOverrides: Map<String, RemoteTranslation>): List<TranslationRecord> {
        val records = mutableListOf<TranslationRecord>()
        for (spec in defaultEntries) {
            val remote = remoteOverrides[spec.key]
            val effectiveEn = remote?.en?.ifBlank { null } ?: spec.en
            val effectiveAm = if (remote != null) remote.am else spec.am
            val effectiveVersion = remote?.enVersion ?: spec.enVersion
            val status = evaluateStatus(
                en = effectiveEn,
                am = effectiveAm,
                remoteEnVersion = effectiveVersion,
                bundledEnVersion = spec.enVersion,
                bundledEn = spec.en,
                isOverridden = remote != null,
            )
            records.add(
                TranslationRecord(
                    key = spec.key,
                    category = spec.category,
                    en = effectiveEn,
                    am = effectiveAm,
                    defaultEn = spec.en,
                    defaultAm = spec.am,
                    enVersion = effectiveVersion,
                    currentEnVersion = spec.enVersion,
                    isCustomized = remote != null,
                    status = status,
                    updatedAt = remote?.updatedAt,
                ),
            )
        }
        for ((key, remote) in remoteOverrides) {
            if (defaultByKey.containsKey(key)) continue
            val cat = TranslationCategory.entries.firstOrNull {
                it.key.equals(remote.category, ignoreCase = true) ||
                    it.name.equals(remote.category, ignoreCase = true)
            } ?: TranslationCategory.MANAGE
            val status = evaluateStatus(
                en = remote.en,
                am = remote.am,
                remoteEnVersion = remote.enVersion,
                bundledEnVersion = remote.enVersion,
                bundledEn = remote.en,
                isOverridden = true,
            )
            records.add(
                TranslationRecord(
                    key = key,
                    category = cat,
                    en = remote.en,
                    am = remote.am,
                    defaultEn = remote.en,
                    defaultAm = "",
                    enVersion = remote.enVersion,
                    currentEnVersion = remote.enVersion,
                    isCustomized = true,
                    status = status,
                    updatedAt = remote.updatedAt,
                ),
            )
        }
        return records
    }

    fun evaluateStatus(
        en: String,
        am: String,
        remoteEnVersion: Int,
        bundledEnVersion: Int,
        bundledEn: String,
        isOverridden: Boolean,
    ): TranslationStatus {
        val cleanAm = am.trim()
        val cleanEn = en.trim()
        return when {
            cleanAm.isEmpty() -> TranslationStatus.MISSING
            cleanAm.equals(cleanEn, ignoreCase = true) -> TranslationStatus.UNTRANSLATED
            isOverridden && (remoteEnVersion < bundledEnVersion || !cleanEn.equals(bundledEn.trim(), ignoreCase = false) && remoteEnVersion <= bundledEnVersion && bundledEn != cleanEn && remoteEnVersion < bundledEnVersion) ->
                TranslationStatus.OUTDATED
            isOverridden && remoteEnVersion < bundledEnVersion -> TranslationStatus.OUTDATED
            else -> TranslationStatus.TRANSLATED
        }
    }

    fun serializeOverrides(overrides: Map<String, RemoteTranslation>): String {
        val arr = JSONArray()
        overrides.values.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("key", entry.key)
                    put("category", entry.category)
                    put("en", entry.en)
                    put("am", entry.am)
                    put("enVersion", entry.enVersion)
                    put("updatedBy", entry.updatedBy)
                },
            )
        }
        return arr.toString()
    }

    fun deserializeOverrides(rawJson: String?): Map<String, RemoteTranslation> {
        if (rawJson.isNullOrBlank()) return emptyMap()
        return try {
            val arr = JSONArray(rawJson)
            val result = LinkedHashMap<String, RemoteTranslation>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val key = obj.optString("key").trim()
                if (key.isEmpty()) continue
                result[key] = RemoteTranslation(
                    key = key,
                    category = obj.optString("category"),
                    en = obj.optString("en"),
                    am = obj.optString("am"),
                    enVersion = obj.optInt("enVersion", 1),
                    updatedBy = obj.optString("updatedBy"),
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
