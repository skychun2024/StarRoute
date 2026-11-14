package com.example.starroute

internal data class StarInfo(
    val id: String,
    val name: String,
    val story: String
)

internal val starCatalog = listOf(
    StarInfo(
        id = "orion",
        name = "오리온자리",
        story = """
            밤하늘에서 나란히 빛나는 세 개의 별을 찾아보세요.
            이 별들은 오리온자리의 허리띠를 이루고 있어요.

            그리스 신화에서 오리온은 뛰어난 사냥꾼이었습니다.

            오리온의 죽음에 대해서는 여러 이야기가
            전해지는데, 그중에는 전갈에게 목숨을
            잃었다는 이야기도 있습니다.

            그래서 오리온자리와 전갈자리는 서로를
            피해 밤하늘에 나타난다는 전설이 전해집니다.

            오늘 밤에는 오리온의 허리띠를 찾아보세요.
        """.trimIndent()
    ),
    StarInfo(
        id = "cassiopeia",
        name = "카시오페이아자리",
        story = """
            밤하늘에서 영어 알파벳 W처럼 생긴
            별자리를 본 적 있으신가요?

            바로 카시오페이아자리입니다.

            그리스 신화에서 카시오페이아는
            자신의 아름다움을 자랑하던 왕비였어요.

            바다의 요정들보다 자신이 아름답다고
            자랑하다가 바다의 신 포세이돈의
            분노를 샀다고 합니다.

            신화 속 카시오페이아는 하늘의
            별자리가 되었고 의자에 앉은 모습으로
            그려집니다.

            밤하늘에서 W 모양의 다섯 별을 찾아보세요.
            보는 방향에 따라 M처럼 보이기도 합니다.
        """.trimIndent()
    ),
    StarInfo(
        id = "ursa_major",
        name = "큰곰자리",
        story = """
            밤하늘에서 국자 모양으로 빛나는
            일곱 개의 별을 알고 계신가요?

            바로 북두칠성입니다.
            북두칠성은 큰곰자리의 일부입니다.

            그리스 신화에는 칼리스토라는 여인이
            곰으로 변했다는 이야기가 전해집니다.

            칼리스토와 관련된 이야기는 큰곰자리의
            유래를 설명하는 대표적인 신화 중 하나입니다.

            북두칠성의 국자 끝 두 별을 이어
            북쪽으로 연장하면 북극성을 찾는 데
            도움을 받을 수 있습니다.

            오래전 사람들은 이렇게 별을 이용해
            방향을 찾기도 했답니다.
        """.trimIndent()
    ),
    StarInfo("lyra", "거문고자리", "밝은 베가를 찾아보세요. 그리스 신화에서는 오르페우스가 연주한 리라가 하늘의 별자리가 되었다고 전해집니다."),
    StarInfo("cygnus", "백조자리", "데네브에서 이어지는 북십자 모양을 찾아보세요. 데네브는 하늘을 나는 백조의 꼬리에 해당합니다."),
    StarInfo("aquila", "독수리자리", "밝은 알타이르는 독수리자리의 대표 별입니다. 우리에게는 칠월칠석 이야기 속 견우성으로 익숙합니다."),
    StarInfo("bootes", "목동자리", "북두칠성의 손잡이 곡선을 따라가면 주황빛 아르크투루스를 찾을 수 있습니다. 목동자리를 찾는 길잡이 별입니다."),
    StarInfo("virgo", "처녀자리", "처녀자리의 푸른빛 별 스피카를 찾아보세요. 스피카라는 이름은 곡식의 이삭을 뜻합니다."),
    StarInfo("scorpius", "전갈자리", "붉은빛 안타레스는 전갈의 심장에 해당합니다. 전갈의 꼬리는 하늘에서 갈고리처럼 휘어 보입니다."),
    StarInfo("taurus", "황소자리", "주황빛 알데바란은 황소의 눈에 해당합니다. 주변에서 브이자 모양과 플레이아데스 성단도 찾아보세요."),
    StarInfo("leo", "사자자리", "레굴루스에서 시작하는 낫 모양은 사자의 머리와 갈기를 그립니다. 그리스 신화에서는 네메아의 사자와 연결됩니다."),
    StarInfo("canis_major", "큰개자리", "시리우스는 밤하늘에서 가장 밝게 보이는 별입니다. 큰개자리는 사냥꾼 오리온을 따르는 개로 그려지곤 합니다.")
)

// Approximate J2000 coordinates of one guide star per constellation.
internal val guideStars = mapOf(
    "orion" to GuideStar("알닐람", 84.05, -1.20),
    "cassiopeia" to GuideStar("셰다르", 10.13, 56.54),
    "ursa_major" to GuideStar("두베", 165.93, 61.75),
    "lyra" to GuideStar("베가", 279.23, 38.78),
    "cygnus" to GuideStar("데네브", 310.36, 45.28),
    "aquila" to GuideStar("알타이르", 297.70, 8.87),
    "bootes" to GuideStar("아르크투루스", 213.92, 19.18),
    "virgo" to GuideStar("스피카", 201.30, -11.16),
    "scorpius" to GuideStar("안타레스", 247.35, -26.43),
    "taurus" to GuideStar("알데바란", 68.98, 16.51),
    "leo" to GuideStar("레굴루스", 152.09, 11.97),
    "canis_major" to GuideStar("시리우스", 101.29, -16.72)
)
