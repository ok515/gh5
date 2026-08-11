package com.pecmi.studio.domain.model

data class QuoteItem(
    val id: String,
    val text: String,
    val category: String,
    val author: String = ""
)

object QuotesProvider {
    val categories = listOf("Motivations", "Wisdom", "Design & Art", "Arabic Motivations", "Arabic Wisdom")

    val quotes = listOf(
        QuoteItem("1", "The only way to do great work is to love what you do.", "Motivations", "Steve Jobs"),
        QuoteItem("2", "Design is not just what it looks like and feels like. Design is how it works.", "Design & Art", "Steve Jobs"),
        QuoteItem("3", "Simplicity is the ultimate sophistication.", "Wisdom", "Leonardo da Vinci"),
        QuoteItem("4", "Creativity is intelligence having fun.", "Design & Art", "Albert Einstein"),
        QuoteItem("5", "Do standard things in extraordinary ways.", "Motivations", "Anonymous"),
        QuoteItem("6", "Stay hungry, stay foolish.", "Motivations", "Steve Jobs"),
        QuoteItem("7", "Make it simple, but significant.", "Design & Art", "Don Draper"),
        QuoteItem("8", "Every artist was first an amateur.", "Design & Art", "Ralph Waldo Emerson"),

        QuoteItem("9", "النجاح هو مجموع المحاولات الصغيرة المتكررة يوماً بعد يوم.", "Arabic Motivations", "حكمة"),
        QuoteItem("10", "التصميم ليس مجرد شكل، بل تجربة وحياة.", "Arabic Design", "Pecmi"),
        QuoteItem("11", "لا تنتظر الفرصة، بل اصنعها بنفسك.", "Arabic Motivations", "حكمة"),
        QuoteItem("12", "البساطة هي أعلى درجات الرقي.", "Arabic Wisdom", "ليوناردو دافنشي"),
        QuoteItem("13", "العقل كالمظلة، لا يعمل إلا إذا كان مفتوحاً.", "Arabic Wisdom", "حكمة"),
        QuoteItem("14", "سر الإنجاز هو البداية.", "Arabic Motivations", "مارك توين"),
        QuoteItem("15", "الإبداع هو أن ترى ما يراه الجميع وتفكر بما لم يفكر فيه أحد.", "Arabic Motivations", "ألبرت أينشتاين")
    )
}
