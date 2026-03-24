package com.novusforge.astrum.core;

import java.util.List;

/**
 * CommunityIncidentDatabase: Hardcoded dataset of Minecraft community incidents (2009-2026).
 * Powering the Identity Intent Verification (IIV) framework for Project Astrum.
 */
public final class CommunityIncidentDatabase {

    public record IncidentRecord(String creator, String category, int year, String description) {}

    public static final List<IncidentRecord> COMMUNITY_INCIDENT_DATABASE = List.of(
        // Early Era (2009-2015)
        new IncidentRecord("Team Avolition", "Griefing", 2010, "Pioneered social engineering griefing and infiltration tactics under Storm Surge."),
        new IncidentRecord("Bashurverse", "Criminal/Minor Contact", 2015, "Resurfacing of 2004 criminal record involving unlawful transactions with a minor."),
        new IncidentRecord("LionMaker", "Grooming/Arrest", 2015, "Belgian YouTuber arrested for grooming and soliciting explicit photos from minors."),
        new IncidentRecord("L for Lee", "Criminal/Stalking", 2015, "Charged as a sex offender after stalking and harassing women online using fake identities within the Minecraft community."),
        
        // Golden Age & Transition (2016-2019)
        new IncidentRecord("JinBop", "Criminal/Arrest", 2016, "Sentenced to prison for production and sale of child pornography involving a fan."),
        new IncidentRecord("SkyDoesMinecraft", "Abuse/Assault", 2018, "Beginning of decline marked by erratic behavior and toxic workplace allegations."),
        
        // Modern Era (2020-2023)
        new IncidentRecord("Dream", "Cheating", 2020, "Accused of manipulating item drop rates in 1.16 speedruns; admitted to 'unintentional' mod usage in 2021."),
        new IncidentRecord("The Fifth Column", "Griefing", 2020, "Founded group using Copenheimer tool to scan and systematically destroy private servers."),
        new IncidentRecord("CallMeCarson", "Grooming", 2021, "Exchanged explicit messages with a 17-year-old fan while aged 19."),
        new IncidentRecord("Jschlatt", "Miscellaneous", 2021, "Faced intense backlash for using a crudely drawn blackface image in a Jackbox thumbnail."),
        new IncidentRecord("PopularMMOs", "Criminal/Abuse", 2021, "Arrested for domestic battery; charges were later dropped but marked the start of legal issues."),
        new IncidentRecord("SkyDoesMinecraft", "Abuse/Assault", 2022, "Elizabeth 'tell-all' document alleged severe physical/emotional abuse and sexual assault."),
        new IncidentRecord("Dream", "Grooming", 2022, "Faced anonymous Snapchat grooming allegations; vehemently denied claims with counter-evidence."),
        
        // Recent Era (2024-2026)
        new IncidentRecord("GeorgeNotFound", "Sexual Assault", 2024, "Accused by Caitibugzz of inappropriate touching while she was intoxicated; released clarification response."),
        new IncidentRecord("Wilbur Soot", "Abuse", 2024, "Accused by Shubble of physical abuse (non-consensual biting) and emotional neglect."),
        new IncidentRecord("Punz", "Racism", 2024, "Accused of using a racial slur (B-slur) against ex-partner of Puerto Rican descent."),
        new IncidentRecord("Skeppy", "Grooming", 2024, "Faced grooming allegations from two individuals; both later retracted statements and deleted documents."),
        new IncidentRecord("PopularMMOs", "Criminal", 2024, "Arrested for resisting an officer with violence and possession; sentenced to 12 months probation."),
        new IncidentRecord("Dream", "Exploitation", 2025, "Public falling out with TommyInnit involving insulting memes and use of advice as leverage."),
        new IncidentRecord("TommyInnit", "Exploitation Dispute", 2025, "Released public video accusing Dream of exploiting young creators, holding YouTube advice as leverage over him since age 16 and claiming Dream SMP success belonged to multiple creators not Dream alone."),
        new IncidentRecord("Skeppy", "Grooming Retracted", 2025, "Both accusers Kaiya and Csyre publicly retracted grooming allegations on July 1 2025 stating claims may have been misinterpreted."),
        new IncidentRecord("Iskall85", "Abuse/Manipulation", 2025, "Allegations of emotional manipulation and inappropriate sexual advances toward community members."),
        new IncidentRecord("Gerg", "Racism/Hate Speech", 2025, "Leaked Discord screenshots alleged racist, homophobic, and transphobic comments."),
        new IncidentRecord("PopularMMOs", "Criminal", 2025, "Multiple arrests for probation violations including failed drug tests and missed service."),
        new IncidentRecord("Dream", "Cheating/Macros", 2026, "Accused of using auto-jump macros for 'tick-perfect' consistency in MCC Parkour Warrior runs."),
        new IncidentRecord("Marlowww", "Identity Fraud/Cheating", 2026, "Alleged to be a male 'catfish' (DangerMario) using AI voice changers and macros to dominate PvP."),
        new IncidentRecord("DangerMario", "Identity Fraud", 2026, "The alleged real identity behind Marlowww; accused of multi-year manipulation of the PvP community.")
    );
}
