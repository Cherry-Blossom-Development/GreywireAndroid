package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireText

// The EULA and Privacy Policy, matching GreywireiPhone's EulaView and
// PrivacyPolicyView. The text is the same as the web versions at
// community.greywirestudio.com; if those change, change these (and iOS) too.
//
// Section bodies use a tiny markup: **bold**, and [[label]] for an in-app link.

private const val EFFECTIVE_DATE = "November 20, 2026"

private data class LegalSection(val number: Int, val title: String, val body: String)

/**
 * The EULA. With a [footer] (the accept step, see EulaGateScreen) and no
 * [onBack], it's the screen a user must get past before Home.
 */
@Composable
fun EulaScreen(
    onBack: (() -> Unit)?,
    onOpenPrivacyPolicy: () -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    LegalDocumentScreen(
        barTitle = "EULA",
        title = "End User License Agreement",
        intro = EULA_INTRO,
        sections = EULA_SECTIONS,
        onBack = onBack,
        onLink = { onOpenPrivacyPolicy() },
        testTag = "eula-screen",
        footer = footer
    )
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        barTitle = "Privacy Policy",
        title = "Privacy Policy",
        intro = PRIVACY_INTRO,
        sections = PRIVACY_SECTIONS,
        onBack = onBack,
        onLink = {},
        testTag = "privacy-screen"
    )
}

@Composable
private fun LegalDocumentScreen(
    barTitle: String,
    title: String,
    intro: String,
    sections: List<LegalSection>,
    onBack: (() -> Unit)?,
    onLink: (String) -> Unit,
    testTag: String,
    footer: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        GreywireTopBar(title = barTitle, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag(testTag),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = GreywireText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Text("Greywire Studio community", style = MaterialTheme.typography.bodyLarge, color = GreywireMuted)
                Text("Effective Date: $EFFECTIVE_DATE", style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
            }

            // Intro, in a copper-edged card
            val shape = RoundedCornerShape(10.dp)
            Text(
                text = intro,
                style = MaterialTheme.typography.bodyMedium,
                color = GreywireMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreywireSurface, shape)
                    .border(1.dp, GreywireCopper.copy(alpha = 0.5f), shape)
                    .padding(16.dp)
            )

            sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${section.number}. ${section.title}",
                        style = MaterialTheme.typography.titleLarge,
                        color = GreywireText,
                        modifier = Modifier.semantics { heading() }
                    )
                    HorizontalDivider(color = GreywireLine, thickness = 1.dp, modifier = Modifier.height(1.dp))
                    Text(
                        text = legalMarkup(section.body, onLink),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreywireMuted
                    )
                }
            }
        }

        footer?.invoke()
    }
}

/** Turns **bold** and [[link]] markup into an AnnotatedString. */
private fun legalMarkup(text: String, onLink: (String) -> Unit): AnnotatedString = buildAnnotatedString {
    val token = Regex("""\*\*(.+?)\*\*|\[\[(.+?)]]""")
    var last = 0
    for (match in token.findAll(text)) {
        append(text.substring(last, match.range.first))
        val bold = match.groups[1]?.value
        val link = match.groups[2]?.value
        if (bold != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = GreywireText)) { append(bold) }
        } else if (link != null) {
            withLink(
                LinkAnnotation.Clickable(
                    tag = link,
                    styles = TextLinkStyles(SpanStyle(color = GreywireCopper)),
                    linkInteractionListener = { onLink(link) }
                )
            ) { append(link) }
        }
        last = match.range.last + 1
    }
    append(text.substring(last))
}

// ---------------------------------------------------------------------------
// EULA
// ---------------------------------------------------------------------------

private const val EULA_INTRO = "This End User License Agreement (\"EULA\" or \"Agreement\") is a legal agreement between you (\"User,\" \"you,\" or \"your\") and Cherry Blossom Development LLC (\"Company,\" \"we,\" \"us,\" or \"our\"), governing your use of the Greywire Studio community at community.greywirestudio.com and associated services (collectively, the \"Service\"). By creating an account or using the Service in any way, you acknowledge that you have read, understood, and agree to be bound by this Agreement. If you do not agree, do not use the Service."

private val EULA_SECTIONS = listOf(
    LegalSection(1, "Acceptance and Eligibility",
        "You must be at least **18 years of age** to use the Service. By using the Service, you represent and warrant that you meet this requirement. We reserve the right to terminate any account found to belong to a minor.\n\nYour use of the Service is also subject to our [[Privacy Policy]], which is incorporated into this Agreement by reference."),
    LegalSection(2, "License Grant",
        "Subject to your compliance with this Agreement, Cherry Blossom Development LLC grants you a limited, non-exclusive, non-transferable, revocable license to access and use the Service for your personal, non-commercial purposes. This license does not include the right to sublicense, sell, resell, transfer, assign, or otherwise exploit the Service or any of its content."),
    LegalSection(3, "Free Accounts, Subscriptions and Greywire Membership",
        "Anyone with a full account can use the community. Free accounts have limits on recordings and bands. A subscription removes them; it is the same subscription as Prosaurus, so one subscription covers both sites. Greywire members have no limits and pay nothing: membership is free and comes with a Greywire Studio pedal. You become a member by claiming your pedal's serial number, or by invitation from us.\n\n**One account per serial.** Each serial number can be claimed by one account. Claim only a pedal you own. Claiming a serial that isn't yours, or sharing one so others can avoid the limits, is a violation of this Agreement.\n\n**Membership follows the pedal.** Membership belongs to the account that holds the serial. If you sell or give away your pedal, contact us and we can move the serial to the new owner's account; your account then loses membership, and the free limits apply again, unless you hold another pedal, an invitation or a subscription.\n\n**Revocation.** We may revoke membership for any violation of this Agreement.\n\n**The community only.** Membership covers the online community. It does not change the terms or warranty of the pedal itself, and the pedal works without it."),
    LegalSection(4, "One Account for Greywire and Prosaurus",
        "Your account is shared with Prosaurus (prosaurus.com), another service run by Cherry Blossom Development LLC. You sign in to both with the same username and password, and your profile, friends and direct messages are the same on both sites. Chat rooms, recordings and bands belong to the site where they were created and are not shown on the other.\n\nThis Agreement covers your account on both sites. A subscription also covers your account on both sites. A suspension or termination applies to the whole account."),
    LegalSection(5, "Prohibited Content - Zero Tolerance Policy",
        "We maintain a **zero-tolerance policy** for objectionable content. The following types of content are strictly prohibited and will result in immediate account termination without warning or refund:\n\n• Sexually explicit or pornographic content of any kind.\n\n• Content that exploits, endangers, or sexualizes minors in any way. Such content will be reported to the NCMEC and relevant law enforcement.\n\n• Hate speech or discriminatory content targeting individuals or groups based on race, ethnicity, religion, gender, sexual orientation, disability, national origin, or any other protected characteristic.\n\n• Content that promotes, glorifies, or incites violence, self-harm, terrorism, or illegal activity.\n\n• Harassment, threats, or intimidation directed at any person or group.\n\n• Spam, misinformation, or deliberately false content intended to deceive or mislead other users.\n\n• Content that violates any applicable law, including copyright, defamation, privacy, or consumer protection laws.\n\n• Malware, phishing links, or any content designed to compromise the security of other users or systems."),
    LegalSection(6, "Prohibited Conduct - Zero Tolerance for Abusive Users",
        "We maintain a **zero-tolerance policy** for abusive behavior. The following conduct is strictly prohibited and will result in immediate and permanent account termination:\n\n• Harassment or bullying of other users, including repeated unwanted contact, public humiliation, or coordinated attacks.\n\n• Threatening or intimidating any user, employee, or person associated with the Service.\n\n• Impersonating another person, company, or entity in a misleading or harmful way.\n\n• Unauthorized access or hacking - attempting to access accounts, systems, or data that are not your own.\n\n• Circumventing or undermining security measures, including sharing login credentials or attempting to bypass account verification.\n\n• Deliberately disrupting the Service, including denial-of-service attacks, flooding, or other technically abusive behavior.\n\n• Creating multiple accounts to evade a ban or circumvent any restriction placed on your access.\n\n• Collecting or scraping user data without authorization.\n\nUsers who engage in any of the above conduct will be permanently banned. We cooperate fully with law enforcement when conduct may constitute a criminal offense."),
    LegalSection(7, "Content Ownership and Responsibility",
        "You retain ownership of content you create and post on the Service. By posting content, you grant Cherry Blossom Development LLC a worldwide, royalty-free, non-exclusive license to host, store, transmit, display, and distribute that content solely for the purpose of operating and improving the Service.\n\nYou are solely responsible for all content you post. We do not endorse any user-submitted content and expressly disclaim all liability arising from it."),
    LegalSection(8, "Reporting Violations",
        "If you encounter content or behavior that violates this Agreement, please report it immediately to:\n\nabuse@cherryblossomdevelopment.com\n\nWhen content is reported or detected by our automated systems, it is immediately hidden from all users pending review. Reports submitted in good faith will be treated confidentially. We do not tolerate retaliation against users who report violations."),
    LegalSection(9, "Automated Content Moderation",
        "To help maintain a safe environment, the Service uses automated tools to review user-submitted content for violations of this Agreement. These tools include:\n\n• Keyword filtering - content is scanned against a list of prohibited terms maintained by Cherry Blossom Development LLC.\n\n• OpenAI Moderation API - content is submitted to OpenAI's moderation service, which analyzes text for hate speech, harassment, sexual content, violence, and related categories. This processing is performed by OpenAI, L.L.C., a third-party service provider.\n\nContent flagged by either system may be automatically hidden and queued for human review. Automated moderation is not perfect - false positives can occur. If you believe your content was incorrectly removed, you may contact us at abuse@cherryblossomdevelopment.com to request a review.\n\nBy using the Service, you consent to your user-submitted content being processed by these automated systems as described above."),
    LegalSection(10, "Enforcement and Account Termination",
        "Cherry Blossom Development LLC reserves the right to suspend or permanently terminate any account, at any time and without prior notice, for any violation of this Agreement or for any conduct we determine to be harmful.\n\nUpon termination: your license to use the Service is immediately revoked, access to your account and all associated content will be disabled, any Greywire membership on the account ends, active subscription fees are non-refundable in cases of policy violation, and we may retain records of your account and activity as required by law or for abuse prevention."),
    LegalSection(11, "Disclaimer of Warranties",
        "The Service is provided \"as is\" and \"as available,\" without warranties of any kind, express or implied. To the fullest extent permitted by law, Cherry Blossom Development LLC disclaims all warranties, including but not limited to merchantability, fitness for a particular purpose, and non-infringement. We do not warrant that the Service will be uninterrupted, error-free, or free of viruses or other harmful components."),
    LegalSection(12, "Limitation of Liability",
        "To the maximum extent permitted by applicable law, Cherry Blossom Development LLC and its officers, directors, employees, and agents shall not be liable for any indirect, incidental, special, consequential, or punitive damages arising out of or related to your use of the Service, even if advised of the possibility of such damages."),
    LegalSection(13, "Governing Law",
        "This Agreement is governed by and construed in accordance with the laws of the State of Washington, United States, without regard to its conflict-of-law provisions. Any disputes arising under this Agreement shall be resolved exclusively in the state or federal courts located in Spokane County, Washington, and you consent to personal jurisdiction in those courts."),
    LegalSection(14, "Changes to This Agreement",
        "We may update this EULA at any time. When we make material changes, we will update the effective date at the top of this page. Your continued use of the Service after any changes constitutes your acceptance of the updated Agreement. We encourage you to review this page periodically."),
    LegalSection(15, "Contact Us",
        "If you have questions or concerns about this Agreement, please contact us:\n\nCherry Blossom Development LLC\nSpokane, Washington\nUnited States\nlegal@cherryblossomdevelopment.com")
)

// ---------------------------------------------------------------------------
// Privacy Policy
// ---------------------------------------------------------------------------

private const val PRIVACY_INTRO = "This Privacy Policy describes how Cherry Blossom Development LLC (\"we,\" \"us,\" or \"our\"), based in Spokane, Washington, collects, uses, and protects your information when you use the Greywire Studio community at community.greywirestudio.com (the \"Community\"). By using the Community, you agree to the practices described in this policy."

private val PRIVACY_SECTIONS = listOf(
    LegalSection(1, "Age Requirement",
        "The Community is intended for use by adults only. You must be at least **18 years of age** to create an account or use this Community. We do not knowingly collect personal information from anyone under the age of 18. If we become aware that a user under 18 has provided us with personal information, we will promptly delete that account and its associated data. If you believe a minor has registered, please contact us at the address listed below."),
    LegalSection(2, "Information We Collect",
        "We collect information you provide directly, as well as certain information automatically:\n\n**Information You Provide**\n\n• **Account Information:** Username, email address, and password when you register.\n\n• **Profile Information:** Display name, profile photo, bio, and any other details you choose to add to your profile.\n\n• **User Content:** Chat messages, recordings, band pages, setlists, comments, ratings, and other content you create or share within the Community.\n\n• **Pedal Information:** The serial number and model of any Greywire pedal you claim, and when you claimed it.\n\n• **Payment Information:** Greywire membership is free, and we collect no payment details for it. If you subscribe (one subscription covers Greywire and Prosaurus), billing details are processed by our payment provider or app store. We do not store full payment card numbers on our servers.\n\n• **Communications:** Any information you send us via email or support requests.\n\n**Information Collected Automatically**\n\n• **Browser Information:** Browser type, operating system, and device type.\n\n• **Usage Data:** Features accessed, content viewed, and interactions within the Community.\n\n• **Log Data:** IP address, access times, and error reports to help us diagnose and fix technical issues.\n\n• **Authentication Tokens:** Session tokens stored securely to keep you logged in."),
    LegalSection(3, "How We Use Your Information",
        "We use the information we collect to:\n\n• Provide, operate, and improve the Community and its features.\n\n• Authenticate your identity and maintain the security of your account.\n\n• Check pedal serial claims and manage memberships.\n\n• Send you service-related notifications (e.g., account verification, password resets, friend activity).\n\n• Respond to your support requests and communications.\n\n• Monitor and enforce our Terms of Service, including age restrictions.\n\n• Detect and prevent fraud, abuse, or other harmful activity.\n\n• Analyze usage trends to improve the user experience.\n\nWe do not sell your personal information to third parties. We do not use your data for behavioral advertising or share it with advertisers."),
    LegalSection(4, "How We Share Your Information",
        "We may share your information only in the following limited circumstances:\n\n• **With Other Users:** Your username and profile are visible to other Greywire users. Recordings you share with a public link can be heard by anyone who has the link.\n\n• **With Prosaurus:** Your account is shared with Prosaurus (prosaurus.com), another service run by Cherry Blossom Development LLC. Your profile, friends, and direct messages are the same on both sites. Chat rooms, recordings, and bands stay on the site where they were created.\n\n• **Service Providers:** We work with trusted third-party vendors who assist us in operating the Community (e.g., payment processing, cloud hosting, email delivery). These providers are contractually obligated to protect your information and may not use it for their own purposes.\n\n• **Legal Requirements:** We may disclose your information if required to do so by law, court order, or governmental authority, or when we believe disclosure is necessary to protect the rights or safety of our users or the public.\n\n• **Business Transfers:** In the event of a merger, acquisition, or sale of company assets, your information may be transferred as part of that transaction. We will notify you of any such change."),
    LegalSection(5, "Data Retention",
        "We retain your personal information for as long as your account is active or as needed to provide you with services. If you delete your account, we will delete or anonymize your personal data within a reasonable period, except where we are required to retain it for legal or legitimate business purposes (such as resolving disputes or complying with legal obligations).\n\nContent you have shared that other users have interacted with (such as comments on a post) may remain in a modified form after account deletion to preserve the integrity of those interactions."),
    LegalSection(6, "Data Security",
        "We take reasonable technical and organizational measures to protect your information from unauthorized access, loss, or misuse. These measures include encrypted connections (HTTPS/TLS), secure password hashing, and access controls on our servers. However, no method of transmission over the internet or electronic storage is completely secure, and we cannot guarantee absolute security.\n\nYou are responsible for keeping your password confidential. Do not share your login credentials with anyone."),
    LegalSection(7, "Your Rights and Choices",
        "You have the following rights regarding your personal information:\n\n• **Access:** You may request a copy of the personal information we hold about you.\n\n• **Correction:** You may update or correct inaccurate information through your account settings.\n\n• **Deletion:** You may request that we delete your account and associated personal data.\n\n• **Portability:** You may request an export of your data in a commonly used format.\n\n• **Opt-Out of Notifications:** You can manage notification preferences within the Community settings.\n\nTo exercise any of these rights, please contact us at the address listed in Section 10. We will respond to your request within a reasonable timeframe."),
    LegalSection(8, "Third-Party Links and Services",
        "The Community may contain links to external websites or services not operated by us. We are not responsible for the privacy practices of those third parties. We encourage you to review the privacy policies of any external services you access."),
    LegalSection(9, "Changes to This Policy",
        "We may update this Privacy Policy from time to time to reflect changes in our practices or legal requirements. When we make material changes, we will update the effective date at the top of this page and, where appropriate, notify you through the Community or via email. Your continued use of the Community after any changes constitutes your acceptance of the updated policy."),
    LegalSection(10, "Contact Us",
        "If you have questions, concerns, or requests regarding this Privacy Policy or your personal data, please contact us:\n\nCherry Blossom Development LLC\nSpokane, Washington\nUnited States\nprivacy@cherryblossomdevelopment.com")
)
