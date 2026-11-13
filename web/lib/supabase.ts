import { createClient } from "@supabase/supabase-js";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://xmbiaajnljiqvmzhjpcl.supabase.co";
const supabaseKey = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY || "sb_publishable_DLUtEPWHDKO73PYsem--ww_er6L5nci";

export const supabase = createClient(supabaseUrl, supabaseKey);

export interface ThreatLogRecord {
  id?: string;
  created_at?: string;
  source_number: string;
  threat_type: string;
  scam_probability: number;
  confidence_score: number;
  dossier_hash?: string;
  status: "intercepted" | "investigating" | "reported_1930";
}

export async function logThreatEvent(event: Omit<ThreatLogRecord, "id" | "created_at">) {
  try {
    const { data, error } = await supabase.from("threat_logs").insert([event]).select();
    if (error) {
      console.warn("Supabase threat logging fallback:", error.message);
    }
    return data;
  } catch (err) {
    console.warn("Supabase network offline or table pending:", err);
    return null;
  }
}
