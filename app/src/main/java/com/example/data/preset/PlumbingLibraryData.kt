package com.example.data.preset

import com.example.data.model.PlumbingMaterial
import com.example.data.model.WorkType

object PlumbingLibraryData {

    val CATEGORIES = listOf(
        "الكل" to "Tous",
        "PPR" to "PPR",
        "PVC" to "PVC",
        "Robinetterie" to "Robinetterie",
        "Sanitaire" to "Sanitaire",
        "Accessoires" to "Accessoires"
    )

    val COMMON_SIZES_PPR = listOf("20mm", "25mm", "32mm", "40mm", "50mm", "63mm", "75mm", "90mm", "110mm")
    val COMMON_SIZES_PVC = listOf("32mm", "40mm", "50mm", "75mm", "90mm", "110mm", "125mm", "160mm")
    val THREAD_SIZES = listOf("1/2\" (15x21)", "3/4\" (20x27)", "1\" (26x34)", "1\"1/4 (33x42)", "1\"1/2 (40x49)", "2\" (50x60)")
    val UNITS = listOf("قطعة", "متر", "علبة", "كيس", "رول", "مجموعة", "وحدة")

    val ALL_MATERIALS: List<PlumbingMaterial> = listOf(
        // PPR
        PlumbingMaterial("ppr_tube", "أنبوب PPR", "Tube PPR", "PPR", COMMON_SIZES_PPR, "25mm", "متر", 250.0, "pipe", null, "أنبوب لحام حراري صلب نقي للمياه الساخنة والباردة"),
        PlumbingMaterial("ppr_coude_90", "كوع PPR 90°", "Coude PPR 90°", "PPR", COMMON_SIZES_PPR, "25mm", "قطعة", 45.0, "elbow", null, "كوع PPR زاوية قائمة 90 درجة"),
        PlumbingMaterial("ppr_coude_45", "كوع PPR 45°", "Coude PPR 45°", "PPR", COMMON_SIZES_PPR, "25mm", "قطعة", 50.0, "elbow", null, "كوع PPR زاوية 45 درجة"),
        PlumbingMaterial("ppr_te", "Tee PPR مفرع", "Té Egal PPR", "PPR", COMMON_SIZES_PPR, "25mm", "قطعة", 60.0, "tee", null, "وصلة ثلاثية PPR متساوية"),
        PlumbingMaterial("ppr_manchon", "وصلة مباشرة PPR (Manchon)", "Manchon PPR", "PPR", COMMON_SIZES_PPR, "25mm", "قطعة", 30.0, "fitting", null, "وصلة مستقيمة للربط بين أنبوبين"),
        PlumbingMaterial("ppr_coude_filete", "كوع سن سن أنثى PPR", "Coude Femelle PPR", "PPR", listOf("20x1/2\"", "25x1/2\"", "25x3/4\"", "32x1\""), "25x1/2\"", "قطعة", 180.0, "elbow", null, "كوع PPR بسن معدني لتركيب الخلاطات والحنفيات"),
        PlumbingMaterial("ppr_vanne", "محبس PPR دفن", "Vanne à Encastrer PPR", "PPR", listOf("20mm", "25mm", "32mm"), "25mm", "قطعة", 750.0, "valve", null, "صمام قطع للدفن تحت البلاط"),
        PlumbingMaterial("ppr_reduction", "منقص PPR (Réduction)", "Réduction PPR", "PPR", listOf("25x20", "32x25", "40x32", "50x40"), "25x20", "قطعة", 40.0, "fitting", null, "وصلة تخفيض القطر"),

        // PVC
        PlumbingMaterial("pvc_tube", "أنبوب PVC صرف", "Tube PVC Evacuation", "PVC", COMMON_SIZES_PVC, "110mm", "متر", 450.0, "pipe", null, "أنبوب PVC رمادي عالي الجودة للصرف الصحي"),
        PlumbingMaterial("pvc_coude_87", "كوع PVC 87°", "Coude PVC 87°", "PVC", COMMON_SIZES_PVC, "110mm", "قطعة", 120.0, "elbow", null, "كوع PVC صرف زاوية قائمة"),
        PlumbingMaterial("pvc_coude_45", "كوع PVC 45°", "Coude PVC 45°", "PVC", COMMON_SIZES_PVC, "110mm", "قطعة", 110.0, "elbow", null, "كوع PVC صرف زاوية مائلة"),
        PlumbingMaterial("pvc_te", "Tee PVC 87°", "Té PVC 87°", "PVC", COMMON_SIZES_PVC, "110mm", "قطعة", 180.0, "tee", null, "وصلة ثلاثية PVC للصرف"),
        PlumbingMaterial("pvc_y_te", "Culotte Y PVC", "Culotte PVC 45°", "PVC", COMMON_SIZES_PVC, "110mm", "قطعة", 220.0, "tee", null, "وصلة Y مائلة للصرف الرئيسي"),
        PlumbingMaterial("pvc_siphon", "سيفون أرضية PVC (Regard)", "Siphon de Sol", "PVC", listOf("100x100", "150x150", "200x200"), "150x150", "قطعة", 650.0, "drain", null, "سيفون أرضية لمنع الرائحة الكريهة"),

        // Robinetterie & Sanitaire
        PlumbingMaterial("robinet_arret", "محبس زاوية (Robinet d'arrêt)", "Robinet d'Arrêt 1/2-3/8", "Robinetterie", THREAD_SIZES, "1/2\" (15x21)", "قطعة", 450.0, "valve", null, "محبس إغلاق للمرحاض والحنفية"),
        PlumbingMaterial("flexible_inox", "أنبوب مرن Inox (Flexible)", "Flexible Inox 40cm", "Robinetterie", listOf("30cm", "40cm", "50cm", "60cm"), "40cm", "قطعة", 280.0, "pipe", null, "وصلة مرنة ستانلس ستيل للخلاطات والخزانات"),
        PlumbingMaterial("vanne_principale", "محبس كرة فراشة (Vanne à bille)", "Vanne à Bille 1\"", "Robinetterie", THREAD_SIZES, "1\" (26x34)", "قطعة", 850.0, "valve", null, "محبس نحاسي رئيسي للعداد والشبكة"),
        PlumbingMaterial("siphon_lavabo", "سيفون المغسلة (Siphon Lavabo)", "Siphon Lavabo Plastic", "Sanitaire", listOf("قياسي"), "قياسي", "قطعة", 350.0, "drain", null, "سيفون تصريف مياه المغسلة"),
        PlumbingMaterial("siphon_evier", "سيفون المجلى (Siphon Évier)", "Siphon Évier Double", "Sanitaire", listOf("مفرد", "مزدوج"), "مفرد", "قطعة", 550.0, "drain", null, "سيفون مجلى المطبخ مع مخرج الغسالة"),
        PlumbingMaterial("pipe_wc", "أنبوب توصيل مرحاض (Pipe WC)", "Pipe WC Extensible", "Sanitaire", listOf("صلب", "مرن أكورديون"), "مرن أكورديون", "قطعة", 480.0, "pipe", null, "وصلة صرف المرحاض بالأنبوب الرئيسي"),
        PlumbingMaterial("bonde_douche", "مصرف دوش (Bonde de Douche)", "Bonde Douche Clic-Clac", "Sanitaire", listOf("90mm", "60mm"), "90mm", "قطعة", 850.0, "drain", null, "مصرف حوض الدوش الدائري"),

        // Accessoires
        PlumbingMaterial("teflon", "شريط تفلون (Téflon)", "Ruban Téflon Pro", "Accessoires", listOf("12mmx12m", "19mmx15m"), "12mmx12m", "قطعة", 50.0, "tool", null, "شريط منع تسريب رزوز النحاس"),
        PlumbingMaterial("colle_pvc", "غراء PVC (Colle PVC)", "Colle PVC 250g", "Accessoires", listOf("125g", "250g", "500g"), "250g", "علبة", 380.0, "tool", null, "غراء لاصق للأنابيب البلاستيكية PVC"),
        PlumbingMaterial("colliers_fixation", "قفيز تثبيت أطلس (Colliers Atlas)", "Colliers Fixation Double", "Accessoires", COMMON_SIZES_PPR, "25mm", "علبة", 450.0, "tool", null, "أقفار تثبيت الأنابيب على الجدار")
    )

    val WORK_TYPES: List<WorkType> = listOf(
        WorkType(
            key = "BATHROOM",
            titleAr = "تركيب حمام",
            titleFr = "Installation Salle de Bain",
            iconEmoji = "🚿",
            descriptionAr = "شبكة ماء بارد وساخن وصرف صحي كاملة للحمام",
            suggestedMaterialKeys = listOf(
                "ppr_tube", "ppr_coude_90", "ppr_te", "ppr_manchon", "ppr_coude_filete",
                "ppr_vanne", "pvc_tube", "pvc_coude_87", "pvc_siphon", "pipe_wc",
                "siphon_lavabo", "bonde_douche", "robinet_arret", "teflon", "colle_pvc"
            )
        ),
        WorkType(
            key = "KITCHEN",
            titleAr = "تركيب مطبخ",
            titleFr = "Installation Cuisine",
            iconEmoji = "🍳",
            descriptionAr = "توصيلات المجلى، الغسالة، والماء البارد والساخن",
            suggestedMaterialKeys = listOf(
                "ppr_tube", "ppr_coude_90", "ppr_te", "ppr_coude_filete", "ppr_vanne",
                "pvc_tube", "pvc_coude_87", "siphon_evier", "robinet_arret", "flexible_inox", "teflon"
            )
        ),
        WorkType(
            key = "HOUSE_WATER",
            titleAr = "شبكة ماء منزل",
            titleFr = "Réseau d'Eau Maison",
            iconEmoji = "🏠",
            descriptionAr = "توزيع المياه للغرف والشقق من العداد أو الخزان",
            suggestedMaterialKeys = listOf(
                "ppr_tube", "ppr_coude_90", "ppr_coude_45", "ppr_te", "ppr_manchon",
                "ppr_reduction", "ppr_vanne", "vanne_principale", "clapet_anti_retour",
                "teflon", "colliers_fixation"
            )
        ),
        WorkType(
            key = "TOILET_WC",
            titleAr = "تركيب WC",
            titleFr = "Installation WC",
            iconEmoji = "🚽",
            descriptionAr = "تثبيت مرحاض، خزان الشفط، والتغذية بالماء",
            suggestedMaterialKeys = listOf(
                "pipe_wc", "pvc_tube", "pvc_coude_87", "ppr_tube", "ppr_coude_filete",
                "robinet_arret", "flexible_inox", "teflon"
            )
        ),
        WorkType(
            key = "HOT_WATER",
            titleAr = "شبكة ماء ساخن",
            titleFr = "Réseau Eau Chaude",
            iconEmoji = "🔥",
            descriptionAr = "أنابيب عزل حراري PPR ومحابس للسخانات الشمسية والكهربائية",
            suggestedMaterialKeys = listOf(
                "ppr_tube", "ppr_coude_90", "ppr_te", "ppr_raccord_femelle", "ppr_vanne",
                "clapet_anti_retour", "flexible_inox", "teflon"
            )
        ),
        WorkType(
            key = "COLD_WATER",
            titleAr = "شبكة ماء بارد",
            titleFr = "Réseau Eau Froide",
            iconEmoji = "💧",
            descriptionAr = "شبكة التغذية الرئيسية الباردة وصمامات العزل",
            suggestedMaterialKeys = listOf(
                "ppr_tube", "ppr_coude_90", "ppr_te", "ppr_vanne", "vanne_principale", "colliers_fixation"
            )
        ),
        WorkType(
            key = "SEWERAGE",
            titleAr = "شبكة صرف صحي",
            titleFr = "Réseau Évacuation PVC",
            iconEmoji = "🕳️",
            descriptionAr = "خطوط الصرف الصحي الرئيسية والفرعية وأنابيب PVC",
            suggestedMaterialKeys = listOf(
                "pvc_tube", "pvc_coude_87", "pvc_coude_45", "pvc_te", "pvc_y_te",
                "pvc_manchon", "pvc_siphon", "pvc_reduction", "colle_pvc"
            )
        ),
        WorkType(
            key = "BUILDING",
            titleAr = "مشروع عمارة",
            titleFr = "Projet Immeuble",
            iconEmoji = "🏢",
            descriptionAr = "أعمدة الصرف والتغذية الرئيسية للمباني متعددة الطوابق",
            suggestedMaterialKeys = listOf(
                "ppr_tube", "ppr_coude_90", "ppr_te", "ppr_reduction", "ppr_vanne",
                "vanne_principale", "clapet_anti_retour", "pvc_tube", "pvc_coude_45",
                "pvc_y_te", "colle_pvc", "colliers_fixation", "teflon"
            )
        ),
        WorkType(
            key = "VILLA",
            titleAr = "مشروع فيلا",
            titleFr = "Projet Villa",
            iconEmoji = "🏡",
            descriptionAr = "شبكات مياه وصرف متكاملة مع الحدائق والمضخات",
            suggestedMaterialKeys = listOf(
                "ppr_tube", "ppr_coude_90", "ppr_te", "ppr_vanne", "vanne_principale",
                "pvc_tube", "pvc_siphon", "siphon_lavabo", "bonde_douche", "colle_pvc", "teflon"
            )
        ),
        WorkType(
            key = "CUSTOM",
            titleAr = "عمل مخصص",
            titleFr = "Travail Sur Mesure",
            iconEmoji = "🛠️",
            descriptionAr = "اختيار حر لأي مادة وبناء القائمة من الصفر",
            suggestedMaterialKeys = emptyList()
        )
    )
}
