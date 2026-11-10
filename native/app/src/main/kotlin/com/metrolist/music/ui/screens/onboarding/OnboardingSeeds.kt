/**
 * JixOne — curated onboarding seed artists.
 *
 * Every channel id and avatar below was verified against YouTube Music's
 * artist search (WEB_REMIX client), so the first-run grid shows real,
 * famous artist pages — the same approach Spotify's onboarding takes.
 * Genres span the international mainstream plus Persian pop, classical,
 * rap and rock, matching JixOne's Farsi-first audience.
 */

package com.metrolist.music.ui.screens.onboarding

/** A seed artist for the onboarding grid. */
data class SeedArtist(
    /** YouTube Music channel id (UC…), verified. */
    val id: String,
    /** Display name. */
    val name: String,
    /** Avatar url (verified channel art, 544 px square crop). */
    val thumbnail: String,
    /** Genre bucket the artist represents. */
    val genre: String,
)

/**
 * The verified seed pool. Persian genres are listed first and carry double
 * weight in [OnboardingSeedArtists] so a Farsi-first user always sees a
 * healthy share of Iranian music in the first grid.
 */
val OnboardingSeedArtists: List<SeedArtist> by lazy {
    val seeds = mutableListOf<SeedArtist>()

    // --- persian_pop
    seeds.add(SeedArtist("UCw5LO6ba1yNC3cps4Zu_1Hg", "Mohsen Chavoshi", "https://yt3.googleusercontent.com/p6KhtsSPSdbM7FtnaIBmUfobJxptRpgwuzgZdrGR1_lvOWnejDZTwD4MRnjsgUwgiXkinn3DbWw5tIQ1=s544", "persian_pop"))
    seeds.add(SeedArtist("UCuwPHjci4Qkc5jNAUn-39cA", "Sirvan Khosravi", "https://yt3.googleusercontent.com/QbJqe9O-F_QWIcPnRMu8Bf8Re0YCG5WVuQVTPU8HVsqINohhGztqgC7hJTqZxU-qR9HhHhlxsXhLldQ=s544", "persian_pop"))
    seeds.add(SeedArtist("UCEtzUROs8I6AnL29g0L5tZQ", "Ebi", "https://lh3.googleusercontent.com/10lvgm72D1DHZbrMFq2geR3eMl-dsoKZRIcbGeyYCGwoyNWovTwYY5l5V5VFtCnE-jiavBIgGHEO8g=s544", "persian_pop"))
    seeds.add(SeedArtist("UCjTEefXs2uhWUDPLgsLzdCg", "Googoosh", "https://yt3.googleusercontent.com/alM5dTccV7nmc32eUYLyVlr1HfSkB8dQcP6PQDA1a7sWKuH_puOA0xpSZUNVRJAyPB0kR0Na-Q=s544", "persian_pop"))

    // --- persian_classic
    seeds.add(SeedArtist("UCKA1tjWL4dUHs02FrXrjNzA", "Mohammad-Reza Shajarian", "https://yt3.googleusercontent.com/v_H_MiMTUu-JIHvJUIiAIonJ6q_JdubxaR4aF7w2Av1HVuJOqiQPPc9lLDgShiCbLrTO03G0sg=s544", "persian_classic"))
    seeds.add(SeedArtist("UCybwhaE9kSHjlohUZ_f994Q", "Shahram Nazeri", "https://yt3.googleusercontent.com/c__nUQwLAQNg2bMwcJnWIo1U3UEq-IlHGO3qT_XjYj7PmpkXk7gPI-gxRReYZ9PzYae4xLDlhjl7rlqCnA=s544", "persian_classic"))
    seeds.add(SeedArtist("UCPaAnMq4U_oWhtTE_Jk_6jA", "Hossein Alizadeh", "https://yt3.googleusercontent.com/EbL6D-YZVKA5q6jXrPI322pDyn7BeF4H5vqVLKUghFt2I4fp6LjWndaV7geYt0VbHtFv9KtP_cMXZ6uGsg=s544", "persian_classic"))
    seeds.add(SeedArtist("UCca1Ahq8i4oXpKxA9lCHu0Q", "Marzieh", "https://yt3.googleusercontent.com/hLNhQZaBbxWPYHah1ygDxpDabaanLVnnTXLEUoiIwnJgFLrCrCBaYOJ3Tju34HzTiRL_iWeAAolCUcI=s544", "persian_classic"))

    // --- persian_rap
    seeds.add(SeedArtist("UCl8zNrYBc1M-WnYMZxM7OPA", "Hichkas", "https://yt3.googleusercontent.com/1ZAlaeIJk343Y-aZu4K5D6XMnKe8rJL8BpzRN-SpjMU41u9odAUQG_L33MOKalT2ZH_dYuMXYPcUFupM=s544", "persian_rap"))
    seeds.add(SeedArtist("UCLXUZsh--v3c-d0zPR7KloQ", "Bahram", "https://lh3.googleusercontent.com/4MNz9ISBHbCk9EsKlP4F2yVW6R1wQ4wHeliqu8DdhAAw0Z0N9aylYTEf9SCWKwtLEpDQlb97llOlgk8=s544", "persian_rap"))
    seeds.add(SeedArtist("UCqQhiAox0_6YMFl621JWazg", "Reza Pishro", "https://yt3.ggpht.com/ytc/AIdro_mSunDSEfB3ev-wC0iu6_GTMHE7Kuru9l10ftPAhBiPBg=s544", "persian_rap"))
    seeds.add(SeedArtist("UCXaFKiLhyELpB7XTzDEBlqA", "Sadegh", "https://lh3.googleusercontent.com/KSikwuWn2uVpCwErHxPFrFukLf0Z-lxg4uW7O3GMQnifcA7gQUsJIFtQoGtOyXDRnEuOWDXegzFgzYA=s544", "persian_rap"))

    // --- persian_rock
    seeds.add(SeedArtist("UCMWxWEnJlfBbyg7C9kwIUrQ", "Kiosk", "https://yt3.googleusercontent.com/CsHH-uSmZntNXgiIWs042gOBZ17s1TAHW1dWdY_qt4NPNizj89Kb2qLsz1rBCmdfznKFitEY=s544", "persian_rock"))
    seeds.add(SeedArtist("UC-KJH5zoP2xBsCZ_8kykKTA", "Barad", "https://yt3.googleusercontent.com/bSVxLOpJOfBQExRqUssueocqdrpRArW3Apbus2Kkt4OksInQ5KH-inUHqOdUNLs5fXw7Fdd39JjuZCc=s544", "persian_rock"))

    // --- pop
    seeds.add(SeedArtist("UCPC0L1d253x-KuMNwa05TpA", "Taylor Swift", "https://yt3.googleusercontent.com/RCpTA6EXJQyjVFDosWOKa2SMmqkua_lA9mHPDWWciLwgqpZLz-k8rXWRF_367trrQ7up9BUwCbk6kRk=s544", "pop"))
    seeds.add(SeedArtist("UCzVb0SIXp9q9PeKCcFjsBtA", "Dua Lipa", "https://lh3.googleusercontent.com/aFx8s1fTuelgxONGbezmTG0EKR8r82uB5H-Q6ZJtssyCWLJWF8GfZNr4tHo84sXdFCPBKrA4R6zXOss=s544", "pop"))
    seeds.add(SeedArtist("UClYV6hHlupm_S_ObS1W-DYw", "The Weeknd", "https://lh3.googleusercontent.com/U-SAmNOu4TynE818gLCfKsuHZ0U5YNEtO9mrjSI9WCCKERs98LzrCal5kajBBTQNwdcisoB2Bn-pHp4=s544", "pop"))
    seeds.add(SeedArtist("UCRw0x9_EfawqmgDI2IgQLLg", "Adele", "https://lh3.googleusercontent.com/mFjMekiL9JJDrJOSTpAvu264z4DDmYS6pdy51ZCLr98kA256rvxSrIIuNv12kMj8RJW2vtTq1ECDhA=s544", "pop"))

    // --- rock
    seeds.add(SeedArtist("UCEPMVbUzImPl4p8k4LkGevA", "Queen", "https://yt3.googleusercontent.com/_Qx50zW4-diO8SuAKhaccaNLyTvObgE-TQv1jYS4M9Kma6llb3s-IHpBBvuZAkPh88lHvXTQGaWS7aE=s544", "rock"))
    seeds.add(SeedArtist("UCIaFw5VBEK8qaW6nRpx_qnw", "Coldplay", "https://lh3.googleusercontent.com/IOKuXtp8PCQ_Fc-vaRKm3sKIXBxFV51gZheLTH5br-YGnWHFQf_Jywcuk7wbprYRoEbQyS_XZY6-nMJX=s544", "rock"))
    seeds.add(SeedArtist("UCxgN32UVVztKAQd2HkXzBtw", "Linkin Park", "https://lh3.googleusercontent.com/uE72emEZFH3TVtCZoIFYKmnf7vsb42RYQxb4X-lonyqPQuS_mLtKpLfBQ5JdHwUijfQo06BtSB7LoQ=s544", "rock"))
    seeds.add(SeedArtist("UC8Yu1_yfN5qPh601Y4btsYw", "Arctic Monkeys", "https://yt3.googleusercontent.com/kbPRnnOmWPXIb35ygxKvXt2a_745AVUkAUeFMqOUxbKx8T_I0f1JUfK3G43-_xUldK16-KrU2cj43i0=s544", "rock"))

    // --- hiphop
    seeds.add(SeedArtist("UCedvOgsKFzcK3hA5taf3KoQ", "Eminem", "https://lh3.googleusercontent.com/JFI6JZrS-Lco4UdpqDfHY5Wgwy51VXWxmNdI7bCBU5CDlIpN6WWyisZ7MGlpjbrxEGYMFpsqoR_UwcE=s544", "hiphop"))
    seeds.add(SeedArtist("UCU6cE7pdJPc6DU2jSrKEsdQ", "Drake", "https://yt3.googleusercontent.com/MxNjcRJ-uK4Xvx7u90IhEFLQM8x9LIGTA9VCKHq5U4Wn2jOgiWaMtg-qz329SIzqnCyhdCCB3MpdAGs=s544", "hiphop"))
    seeds.add(SeedArtist("UCprAFmT0C6O4X0ToEXpeFTQ", "Kendrick Lamar", "https://yt3.googleusercontent.com/uB8Magh99SvDyT_mcDYeNYxlVZ_F9WN-cJtAFMHw_Q-_N_8y5-uZiay8-EZSKKloNoWxymBzVehSF4PN=s544", "hiphop"))
    seeds.add(SeedArtist("UCf_gP4AMRSgAfyzbkeS9k4g", "Travis Scott", "https://yt3.googleusercontent.com/r9k_FpAswxhQnl_cudiaT2ocWFccR6SzEFXgZ9a12iR5eDPSILlIL2EQewyQ-yYSt1JFyH1pqnoBXxs=s544", "hiphop"))

    // --- electronic
    seeds.add(SeedArtist("UCZ0Aezmtk-S2l8A9Ln-2lKw", "Calvin Harris", "https://yt3.googleusercontent.com/OsW5zo5w26RBwGTLhC4SSNpmt6ufjosU0lzTg2EqlozD5L5Ja2jpP0GWSA06E2bIa8SFflpw=s544", "electronic"))
    seeds.add(SeedArtist("UCh-4c1ZoBPNP1C8c-7iVvUQ", "David Guetta", "https://yt3.googleusercontent.com/3eBIDwhU0lhvxmScIULhDZTLrpIFTW32aCbDbRMabkpxytTWU80JCj_duXrlZMSbF1b-0cmn7I7ZBkc=s544", "electronic"))
    seeds.add(SeedArtist("UCRr1xG_2WIDs18a6cIiCxeA", "Daft Punk", "https://lh3.googleusercontent.com/qLhu6Py_4_xoBsoubKQsXlhOQGqU9YU1ZRAbFusF0LlrPkXbbpu7bEh-k_ZtE4JwLgubvucAQqcK1hRk=s544", "electronic"))
    seeds.add(SeedArtist("UCrxpwXq8wCTskOQq5d_KoqQ", "Marshmello", "https://yt3.googleusercontent.com/FgXNumEMez1nb_08prkNDaGnKChcoTsxfe3dLBQ1bZoDFQ4X1pBnoTbZLeP28dWKkWYxOerVnamKag=s544", "electronic"))

    // --- rnb
    seeds.add(SeedArtist("UCoPQ_TWm8JZ5nJv4a5BzSWA", "Beyoncé", "https://lh3.googleusercontent.com/GAR8Y4xuTIBIrOr0GPSPdhPc-aRvUCOXwiC0O75cKSP7uRoHFvb20K2ZwpaZwxp2IcSoXGBiAgjit-BC=s544", "rnb"))
    seeds.add(SeedArtist("UCeKDV9JgivrXehVluw5bKFA", "SZA", "https://lh3.googleusercontent.com/c-ILO8kXxjY6HhqSkoClWPUtPfHYQW6iHr51EiQOaZiUZ7IZr_WwwkyqclAOFyZgLpC3R0dPXuZiRt0=s544", "rnb"))
    seeds.add(SeedArtist("UCZn4r7heNOPY-C43YIywnVA", "Bruno Mars", "https://lh3.googleusercontent.com/hnefGBrazRhn4Z92bdSZBUENl40ONjRiVDsmZKZh-WZ2iCKE-2c7KKR7SNcZfzLHoRyB3E6as8L87YA=s544", "rnb"))

    // --- jazz
    seeds.add(SeedArtist("UCIev2PktTH5mI-QlGmbUkiw", "Miles Davis", "https://lh3.googleusercontent.com/RgU9lxCfVPGtTHV9ipuqFqk8BfW0F166wKLBkmdGKQX7M_a9ti7FsGGOyZA0lj3rvaOLxtBPRQYYUts=s544", "jazz"))
    seeds.add(SeedArtist("UCH4T2kv7rr9qnuP0DOXLxMA", "John Coltrane", "https://lh3.googleusercontent.com/0Bvhu4NNaOZq_KFArKyQmITvm0B6hEyq5PB2LfxEG0h7KgypRM_FChDqjh2LJIN0m2NHQv-mCk5YH8Q=s544", "jazz"))
    seeds.add(SeedArtist("UCxRuL2yOu2ydTJNZuYdU0qg", "Norah Jones", "https://yt3.googleusercontent.com/DizkvLuVeCKfsX4122BqXErnTYdTKiAyzEGo2Z2l0EEHuTcHmckngGWMU2hvl_wxJFIFZL1CMwZZgRE=s544", "jazz"))

    // --- classical
    seeds.add(SeedArtist("UCinzb98c2WE_NEkfX9pbrcQ", "Ludovico Einaudi", "https://lh3.googleusercontent.com/jXVkJFK66GX4iO55ePoOsdOTWgfxtB-ZicwywM1fpwniwDeVHIdT5wcOSs2g8_YNhyBd77TXrBkodw=s544", "classical"))
    seeds.add(SeedArtist("UCmeFxYk5BSbJGWN9OgTAIhw", "Wolfgang Amadeus Mozart", "https://yt3.googleusercontent.com/pZyWzrMGGncmh-PudR2HH7IN9rcQZFKq3mSryoUXmVY1hyyoSrmlieHaDCnqSdRpWSmJjNgvLT5wtIA=s544", "classical"))
    seeds.add(SeedArtist("UCyTnUReB5s38R-ZKlb2wyVg", "Frédéric Chopin", "https://yt3.googleusercontent.com/k0voQ5Her_EJvKWzawMj4cZyY77xx4K8ZyoaRmMakgBug5hs7Nu4ufgVLDTbmc-oAymgf97xSogLln2e=s544", "classical"))
    seeds.add(SeedArtist("UCfCNL5oajlQBAlyjWv1ChVw", "Hans Zimmer", "https://lh3.googleusercontent.com/9G1WVhCq8K1cn6HrYa8kwUUNmZFWmoEaRwFab7NrlhMg6i1O9GihxVaaRK89ivWAJjPDngiweFBCUTI=s544", "classical"))

    // --- metal
    seeds.add(SeedArtist("UCGexNm_Kw4rdQjLxmpb2EKw", "Metallica", "https://yt3.googleusercontent.com/VqJfYEB0WmZ8sQ4VIP-hlrZYqsOs12NN-VtCFcTFOi9XRqbiZ4Zhb0JZ2d3-fzIctoMR7raDHFeyNoYa=s544", "metal"))
    seeds.add(SeedArtist("UC0zbzp6x7zR8u0LhanNWFyw", "Iron Maiden", "https://yt3.googleusercontent.com/nhiZLMxjoxcIOcjrMr_Es_0ARMq0iCMkcfMivwQZrF5UL0kJwUthJIAH6dnQstu3MbmyyPGX5RazQ9E=s544", "metal"))
    seeds.add(SeedArtist("UCDJftX2zx_UT_QSnBGIF96w", "System of A Down", "https://yt3.googleusercontent.com/jg9t5w2HS_pbCHN3z21mnY1LZzUbasjMJ_INo7TDOUrrqHs3W9e-mdHvvxgeOK1VXpKTq3YK_xEZOw=s544", "metal"))

    // --- kpop
    seeds.add(SeedArtist("UC9vrvNSL3xcWGSkV86REBSg", "BTS", "https://lh3.googleusercontent.com/8rsLpP6VJjt-yTN8ZG5rY-qt2aCC-IYMWmVkk7qa8c5vrjO6zetKSgwO2QknHI3FtWl6Zannp2VsQ5o=s544", "kpop"))
    seeds.add(SeedArtist("UCkbbMCA40i18i7UdjayMPAg", "BLACKPINK", "https://lh3.googleusercontent.com/pvJ1n3OBNXcz80KquAr-BY3ZRpp4Vfv8IHBk1SHBkJPF6eGhYJqaO3H5meVIQwb-BTOv0VsckNtPow=s544", "kpop"))
    seeds.add(SeedArtist("UCxOqS3cYg4FaHbobICo7nFQ", "NewJeans", "https://yt3.googleusercontent.com/JGuyhWDGT1TMJOTXUKCbFZo6ZRHqxXyvTmUXnMVfVx36fhTLM_SLHOn7a2y1mgX_E9ep2dQlepGWgKEE=s544", "kpop"))
    seeds.add(SeedArtist("UCIMmuidNJdncfMEelOU08Fg", "Stray Kids", "https://yt3.googleusercontent.com/22FOVr8D5Kq9y2l2sUDKW5X5s0W_axG7_sDhQVzxDGEXBMNjhwzvhs10BI3UoA1focajEeTPqZ2jNW9D=s544", "kpop"))

    // --- latin
    seeds.add(SeedArtist("UCiY3z8HAGD6BlSNKVn2kSvQ", "Bad Bunny", "https://yt3.googleusercontent.com/ploU_4iWpDoJlX3FOlwwd_yQcex0I8A0_665lePXAEBbNp1zn5g42eNwg5Q7lvYc2mG2--UNIYcIhww=s544", "latin"))
    seeds.add(SeedArtist("UCo6JijJGA3IvIiPsawDK3Ww", "Shakira", "https://yt3.googleusercontent.com/7NhzXEoaIgr-0tYoab-gNZfF8zA8IbpTPt_buovH2Y78pc1L7IfKTq7tGU6zSk9EBJX0yKq7wp3jts8=s544", "latin"))
    seeds.add(SeedArtist("UCWw-Guyr5ul9B-d5kJlHMng", "J Balvin", "https://yt3.googleusercontent.com/uEfoH9sXpsrJuCqHsQ5EYuOWf42zsbpvv32Zd34P8i1f_ebjrcngqaMRfObvIl8Lkij3ggDBIIswnSM=s544", "latin"))
    seeds.add(SeedArtist("UC7n3gWRN0vQzgiOKc51aZ4w", "KAROL G", "https://lh3.googleusercontent.com/Yzqsh_83kXz4dl0dvPlHTFeQjAkKbX3u-TV-9I7mo-bWm4sCKiHcCBTis2GculkVVNGG76Wwk3uQePeD=s544", "latin"))

    // --- arabic
    seeds.add(SeedArtist("UCaA0_e02nT85T8h3xMclMvA", "Amr Diab", "https://yt3.googleusercontent.com/pUe16NPzy38_YPtW-rFqzRJVrcFt0cWqJXKdJ391df5vvgZIjR5_nnCJNRVGjn8Y1bcYUnyC0-OZ5g=s544", "arabic"))
    seeds.add(SeedArtist("UCzixfFiEFMjhSB3R9UdUdsA", "Fairuz", "https://yt3.googleusercontent.com/ZIONSAndglfiCvZdwa0CNCrUFWN6EUvhQxyY6MtqRzzuZQYeg27M80K0LAikAZkmWcTgbSXXkA=s544", "arabic"))
    seeds.add(SeedArtist("UCon_-FivLncfeatzAPn0jWw", "Nancy Ajram", "https://yt3.googleusercontent.com/7vHuDstipBz-7AzNt7uS6L3dl786d8M6tQNi7c5WHphzABzxZ2ccdMDQvSqwlDu-JOsDlgA575WTm_G9=s544", "arabic"))
    seeds.add(SeedArtist("UCLnNBIKnAe3LtVte6OxJE1w", "Tamer Hosny", "https://yt3.googleusercontent.com/cSP4BHue5ZJT959SCuYn5rZaQSOI8fwSWgcM6ApFBngl-_mqlk2cUntE-k3fDYfFqsoZu_LIvQ=s544", "arabic"))

    // --- turkish
    seeds.add(SeedArtist("UCQm-Fc8TAF3c1hJffBPjxgw", "Tarkan", "https://lh3.googleusercontent.com/gMYM_o7yOSZLCrRi6ibDG4A3j-PHZJ_QW3SMnDpolGsYtVtVUp7i6oGGkcc3JC_uVRGEJuR8sUhlpwg=s544", "turkish"))
    seeds.add(SeedArtist("UCNaGLJRPE3ohleIDM7RFtlQ", "Sezen Aksu", "https://lh3.googleusercontent.com/4lUgniUT8z0CeQSZui-1eagl8S9tIF3G70v0AcmeYpUonl-PC8LuDbcSdfp5yYqOm-sFqlDsZFDh1xoU=s544", "turkish"))
    seeds.add(SeedArtist("UCby21dqpwR_c8okl3K85_7A", "Sagopa Kajmer", "https://yt3.googleusercontent.com/Gni2ivtLkng_0gADgIdb0v_YoZeQOEVu2_Ow_AoMu6lE3PHfIPbTaZanyvFJfWCYxG_5BhMHW486Wzo=s544", "turkish"))

    // Persian double weight for the Farsi-first audience.
    seeds.addAll(seeds.filter { it.genre.startsWith("persian") })
    seeds
}
