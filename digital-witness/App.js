import React, { useState, useEffect, useRef } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, TextInput, ScrollView, Modal, Alert, Vibration, Image, ActivityIndicator, Linking } from 'react-native';
import { Camera, CameraView, useCameraPermissions, useMicrophonePermissions } from 'expo-camera';
import { Audio } from 'expo-av';
import * as Speech from 'expo-speech';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { StatusBar } from 'expo-status-bar';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';

// Constants
const STORAGE_KEY_USER = '@user_profile';
const PIN_CODE = '1234';
const SUPABASE_URL = 'https://vglyohhcnhmjqqylpyss.supabase.co/functions/v1/ask-advisor';
const SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZnbHlvaGhjbmhtanFxeWxweXNzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzA0MzIyNDEsImV4cCI6MjA4NjAwODI0MX0.wR2IPYX9IhpkzL0Y-dzYPoz8HdeFvWY7oQX6W-OwqII';

const RIGHTS_MESSAGES = [
  "YOU HAVE THE RIGHT TO REMAIN SILENT",
  "ASK: 'AM I FREE TO GO?'",
  "DO NOT CONSENT TO ANY SEARCHES",
  "REQUEST A LAWYER IMMEDIATELY",
  "YOU DO NOT HAVE TO SIGN ANYTHING",
  "KEEP YOUR HANDS VISIBLE",
];

export default function App() {
  const [currentScreen, setCurrentScreen] = useState('loading'); // loading, onboarding, dashboard, active, summary
  const [userProfile, setUserProfile] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [cameraPermission, requestCameraPermission] = useCameraPermissions();
  const [micPermission, requestMicPermission] = useMicrophonePermissions();
  const [recordingTime, setRecordingTime] = useState(0);
  const [activeSessionId, setActiveSessionId] = useState(null);

  // Load profile on start
  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const jsonValue = await AsyncStorage.getItem(STORAGE_KEY_USER);
      if (jsonValue != null) {
        setUserProfile(JSON.parse(jsonValue));
        setCurrentScreen('dashboard');
      } else {
        setCurrentScreen('onboarding');
      }
    } catch (e) {
      console.error(e);
      setCurrentScreen('onboarding');
    }
  };

  const saveProfile = async (profile) => {
    try {
      await AsyncStorage.setItem(STORAGE_KEY_USER, JSON.stringify(profile));
      setUserProfile(profile);
      setIsEditing(false);
      setCurrentScreen('dashboard');
    } catch (e) {
      Alert.alert('Error', 'Failed to save profile');
    }
  };

  const editProfile = () => {
    Alert.alert(
      'Edit Profile',
      'Do you want to edit your safety profile?',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Edit', onPress: () => { setIsEditing(true); setCurrentScreen('onboarding'); } }
      ]
    );
  };

  // Screen Navigation
  const startSession = async () => {
    // 1. Check Camera
    if (!cameraPermission || !cameraPermission.granted) {
      const newCameraPermission = await requestCameraPermission();
      if (!newCameraPermission.granted) {
        Alert.alert("Permission Required", "Camera is needed for your safety.");
        return;
      }
    }

    // 2. Check Microphone (CRITICAL FIX)
    if (!micPermission || !micPermission.granted) {
      const newMicPermission = await requestMicPermission();
      if (!newMicPermission.granted) {
        Alert.alert(
          "Microphone Needed",
          "To speak with the AI Advisor, please allow microphone access.",
          [{ text: "Open Settings", onPress: () => Linking.openSettings() }, { text: "Cancel" }]
        );
        return;
      }
    }

    setActiveSessionId(Date.now().toString());
    setRecordingTime(0);
    setCurrentScreen('active');
  };

  const endSession = () => {
    setCurrentScreen('summary');
  };

  if (!cameraPermission || !micPermission) {
    // Permissions are still loading
    return <View style={styles.container} />;
  }

  return (
    <View style={styles.container}>
      <StatusBar style="light" />
      {currentScreen === 'loading' && <ActivityIndicator size="large" color="#FF0000" />}
      {currentScreen === 'onboarding' && <OnboardingScreen onSave={saveProfile} initialData={isEditing ? userProfile : null} />}
      {currentScreen === 'dashboard' && <DashboardScreen user={userProfile} onArm={startSession} onEditProfile={editProfile} />}
      {currentScreen === 'active' && <ActiveSessionScreen onEnd={endSession} recordingTime={recordingTime} setRecordingTime={setRecordingTime} />}
      {currentScreen === 'summary' && <SummaryScreen sessionDuration={recordingTime} onHome={() => setCurrentScreen('dashboard')} />}
    </View>
  );
}

// --- DASHBOARD SCREEN ---
function DashboardScreen({ user, onArm, onEditProfile }) {
  const [serverStatus, setServerStatus] = useState("CHECKING...");

  useEffect(() => {
    checkServer();
  }, []);

  const checkServer = async () => {
    console.log("checkServer: Checking status...");
    try {
      const response = await fetch(SUPABASE_URL, {
        method: 'OPTIONS', // Simple ping
        headers: { 'Authorization': `Bearer ${SUPABASE_KEY}` }
      });
      console.log("checkServer: Response status:", response.status);
      if (response.ok) {
        console.log("checkServer: ONLINE");
        setServerStatus("ONLINE");
      } else {
        console.log("checkServer: OFFLINE (Response not OK)");
        setServerStatus("OFFLINE");
      }
    } catch (e) {
      console.log("checkServer: OFFLINE (Error)", e);
      setServerStatus("OFFLINE");
    }
  };

  return (
    <View style={styles.screenContainer}>
      <View style={styles.topBar}>
        <Ionicons name="shield-checkmark" size={24} color={serverStatus === "ONLINE" ? "#00FF00" : "#FF0000"} />
        <Text style={[styles.statusText, { color: serverStatus === "ONLINE" ? "#00FF00" : "#FF0000" }]}>
          SYSTEM: {serverStatus}
        </Text>
      </View>

      <View style={styles.centerContent}>
        <TouchableOpacity style={styles.armButton} onPress={onArm}>
          <View style={styles.armInner}>
            <Text style={styles.armText}>ARM</Text>
          </View>
        </TouchableOpacity>
        <Text style={styles.hintText}>TAP TO ACTIVATE PROTOCOL</Text>
      </View>

      <View style={styles.footer}>
        <Text style={styles.footerText}>User: {user?.name} | {user?.citizenship}</Text>
        <TouchableOpacity style={styles.editProfileButton} onPress={onEditProfile}>
          <Ionicons name="create-outline" size={16} color="#00BFFF" />
          <Text style={styles.editProfileText}>EDIT PROFILE</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

// --- ACTIVE SESSION SCREEN ---
function ActiveSessionScreen({ onEnd, recordingTime, setRecordingTime }) {
  const [modalVisible, setModalVisible] = useState(false);
  const [pin, setPin] = useState('');
  const [scrollingText, setScrollingText] = useState(RIGHTS_MESSAGES[0]);
  const [aiStatus, setAiStatus] = useState("AI LISTENING...");
  const [aiVisionText, setAiVisionText] = useState("Analyzing Scene...");
  const [isTalking, setIsTalking] = useState(false);

  // Timer
  useEffect(() => {
    const interval = setInterval(() => {
      setRecordingTime(prev => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  // Cycling Rights Messages
  useEffect(() => {
    let index = 0;
    const interval = setInterval(() => {
      index = (index + 1) % RIGHTS_MESSAGES.length;
      setScrollingText(RIGHTS_MESSAGES[index]);
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  // Simulated Vision Analysis Loop
  useEffect(() => {
    const visionInterval = setInterval(() => {
      const scenarios = [
        "OFFICER DETECTED",
        "VEHICLE STOP",
        "HIGH STRESS AUDIO",
        "LIGHTING: POOR",
        "MULTIPLE VOICES"
      ];
      const randomScenario = scenarios[Math.floor(Math.random() * scenarios.length)];
      setAiVisionText(`VISION: ${randomScenario}`);
    }, 5000);
    return () => clearInterval(visionInterval);
  }, []);

  // Push to Talk Handlers
  const handlePushToTalkIn = async () => {
    console.log("🎤 Pressed");
    if (isTalking || recordingRef.current) return;

    try {
      const { status } = await Audio.requestPermissionsAsync();
      console.log("Permission:", status);
      if (status !== 'granted') {
        Alert.alert("Mic Required", "Allow mic access", [
          { text: "Cancel" },
          { text: "Settings", onPress: () => Linking.openSettings() }
        ]);
        return;
      }
    } catch (e) {
      console.error("Permission error:", e);
      return;
    }

    setIsTalking(true);
    setAiStatus("LISTENING...");
    Vibration.vibrate(50);

    try {
      await Audio.setAudioModeAsync({ allowsRecordingIOS: true, playsInSilentModeIOS: true });
      const { recording } = await Audio.Recording.createAsync(Audio.RecordingOptionsPresets.HIGH_QUALITY);
      recordingRef.current = recording;
      console.log("Recording started");
    } catch (err) {
      console.error(err);
      setAiStatus("MIC ERROR");
      setIsTalking(false);
      Alert.alert("Error", err.message);
    }
  };

  const recordingRef = useRef(null);

  const handlePushToTalkOut = async () => {
    console.log("🎤 Button Released");
    if (!isTalking) {
      console.log("⚠️ Not talking, ignoring");
      return;
    }

    setIsTalking(false);
    setAiStatus("ANALYZING...");

    const recording = recordingRef.current;
    recordingRef.current = null;

    if (!recording) {
      console.log("⚠️ No recording ref");
      return;
    }

    try {
      console.log("⏹️ Stopping recording...");
      await recording.stopAndUnloadAsync();
      const uri = recording.getURI();
      console.log("📁 URI:", uri);

      const formData = new FormData();
      formData.append('audio', {
        uri: uri,
        type: 'audio/m4a',
        name: 'voice.m4a',
      });

      console.log("📤 Uploading to:", SUPABASE_URL);
      const response = await fetch(SUPABASE_URL, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${SUPABASE_KEY}`,
        },
        body: formData,
      });

      console.log("📥 Response:", response.status, response.statusText);

      if (!response.ok) {
        const errorText = await response.text();
        console.error("❌ Server error:", errorText);
        setAiStatus("SERVER ERROR");
        Alert.alert("Server Error", `Status ${response.status}: ${errorText.substring(0, 100)}`);
        return;
      }

      const data = await response.json();
      console.log("✅ Data:", JSON.stringify(data));

      if (data.advice) {
        console.log("🗣️ Speaking:", data.advice);
        setAiStatus(`ADVICE: ${data.advice}`);
        Speech.speak(data.advice, {
          language: 'en',
          pitch: 1.0,
          rate: 1.1,
        });
      } else {
        console.warn("⚠️ No advice in response");
        setAiStatus("NO ADVICE");
      }

    } catch (error) {
      console.error("❌ Error:", error.message || error);
      setAiStatus("CONNECTION ERROR");
      Alert.alert("Error", `Failed: ${error.message}`);
    }
  };

  const playDeescalation = () => {
    const text = "I am recording this interaction for my safety. I am remaining silent.";
    Speech.speak(text, { language: 'en', rate: 0.9, pitch: 1.0 });
  };

  const contactLawyer = () => {
    Alert.alert("Contacting Legal Counsel", "Connecting you to the nearest available pro-bono attorney...", [
      { text: "OK", onPress: () => console.log("Mock Lawyer Contacted") }
    ]);
    Vibration.vibrate([0, 500, 200, 500]);
  };

  const handlePinChange = (value) => {
    setPin(value);
    if (value.length === 4) {
      if (value === PIN_CODE) {
        setModalVisible(false);
        onEnd();
      } else {
        Alert.alert("Incorrect PIN");
        setPin('');
      }
    }
  };

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins < 10 ? '0' : ''}${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <View style={styles.container}>
      {/* Camera Background */}
      <CameraView style={StyleSheet.absoluteFill} facing="back" />

      {/* Overlay */}
      <View style={styles.overlayContainer}>

        {/* Top Status Bar */}
        <View style={styles.activeTopBar}>
          <View style={styles.recIndicator}>
            <View style={styles.redDot} />
            <Text style={styles.recText}>REC</Text>
          </View>
          <Text style={styles.timerText}>{formatTime(recordingTime)}</Text>
          <View style={styles.uploadIndicator}>
            <Ionicons name="cloud-upload" size={16} color="#00FFFF" />
            <Text style={styles.uploadText}> CLOUD</Text>
          </View>
        </View>

        {/* AI HUD */}
        <View style={styles.aiHud}>
          <Text style={styles.visionText}>{aiVisionText}</Text>
          <View style={styles.rightsBanner}>
            <Text style={styles.rightsText}>{scrollingText}</Text>
          </View>
        </View>

        {/* Center AI Status */}
        <View style={styles.aiStatusContainer}>
          <Text style={[styles.aiStatusText, isTalking && styles.aiStatusActive]}>{aiStatus}</Text>
        </View>

        {/* Bottom Controls */}
        <View style={styles.activeFooter}>
          <TouchableOpacity style={styles.deescalateButton} onPress={playDeescalation}>
            <Ionicons name="megaphone" size={24} color="black" />
            <Text style={styles.deescalateText}>PLAY MSG</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.talkButton, isTalking && styles.talkButtonActive]}
            onPressIn={handlePushToTalkIn}
            onPressOut={handlePushToTalkOut}
            activeOpacity={0.8}
          >
            <Ionicons name={isTalking ? "mic" : "mic-outline"} size={32} color="white" />
            <Text style={styles.talkButtonText}>{isTalking ? "LISTENING" : "HOLD TO ASK"}</Text>
          </TouchableOpacity>

          <View style={{ alignItems: 'center' }}>
            <TouchableOpacity style={styles.stopButton} onPress={() => { setModalVisible(true); setPin(''); }}>
              <View style={styles.stopInner} />
              <Text style={styles.stopText}>STOP</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Lawyer Button - Moved to absolute bottom right for better visibility */}
        <TouchableOpacity style={styles.lawyerButtonFloating} onPress={contactLawyer}>
          <Ionicons name="briefcase" size={24} color="white" />
          <Text style={styles.lawyerButtonText}>LAWYER</Text>
        </TouchableOpacity>

      </View>

      {/* PIN Modal */}
      <Modal visible={modalVisible} transparent={true} animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>ENTER SECURITY PIN</Text>
            <TextInput
              style={styles.pinInput}
              value={pin}
              onChangeText={handlePinChange}
              keyboardType="numeric"
              maxLength={4}
              secureTextEntry
              autoFocus
            />
            <TouchableOpacity style={styles.cancelButton} onPress={() => setModalVisible(false)}>
              <Text style={styles.cancelText}>CANCEL</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </View>
  );
}

// --- ONBOARDING SCREEN (MULTI-STEP) ---
function OnboardingScreen({ onSave, initialData = null }) {
  const [currentStep, setCurrentStep] = useState(1);

  // Step 1: Name & Phone
  const [name, setName] = useState(initialData?.name || '');
  const [phone, setPhone] = useState(initialData?.phone || '');

  // Step 2: Demographics
  const [gender, setGender] = useState(initialData?.gender || '');
  const [dateOfBirth, setDateOfBirth] = useState(initialData?.dateOfBirth || '');
  const [citizenship, setCitizenship] = useState(initialData?.citizenship || '');

  // Step 3: Emergency Contact
  const [emergencyName, setEmergencyName] = useState(initialData?.emergencyName || '');
  const [emergencyPhone, setEmergencyPhone] = useState(initialData?.emergencyPhone || '');

  // Step 4: Lawyer (Optional)
  const [lawyerName, setLawyerName] = useState(initialData?.lawyerName || '');
  const [lawyerPhone, setLawyerPhone] = useState(initialData?.lawyerPhone || '');

  // Step 5: Permissions (placeholders)
  const [permCamera, setPermCamera] = useState(initialData?.permCamera ?? true);
  const [permMic, setPermMic] = useState(initialData?.permMic ?? true);
  const [permPhotos, setPermPhotos] = useState(initialData?.permPhotos ?? true);
  const [permCalls, setPermCalls] = useState(initialData?.permCalls ?? false);
  const [permLocation, setPermLocation] = useState(initialData?.permLocation ?? true);

  // Step 6: PIN
  const [pin, setPin] = useState('');
  const [pinConfirm, setPinConfirm] = useState('');

  const validateStep = () => {
    if (currentStep === 1) {
      if (!name || !phone) {
        Alert.alert('Missing Info', 'Please enter your name and phone number.');
        return false;
      }
    } else if (currentStep === 2) {
      if (!gender || !dateOfBirth || !citizenship) {
        Alert.alert('Missing Info', 'Please complete all demographic fields.');
        return false;
      }
    } else if (currentStep === 3) {
      if (!emergencyName || !emergencyPhone) {
        Alert.alert('Missing Info', 'Please enter emergency contact details.');
        return false;
      }
    } else if (currentStep === 6) {
      if (!pin || pin.length < 4) {
        Alert.alert('Invalid PIN', 'PIN must be at least 4 digits.');
        return false;
      }
      if (pin !== pinConfirm) {
        Alert.alert('PIN Mismatch', 'PIN and confirmation do not match.');
        return false;
      }
    }
    return true;
  };

  const handleNext = () => {
    if (!validateStep()) return;
    if (currentStep < 6) {
      setCurrentStep(currentStep + 1);
    } else {
      // Final save
      onSave({
        name, phone, gender, dateOfBirth, citizenship,
        emergencyName, emergencyPhone, lawyerName, lawyerPhone,
        permCamera, permMic, permPhotos, permCalls, permLocation, pin
      });
    }
  };

  const handlePrevious = () => {
    if (currentStep > 1) setCurrentStep(currentStep - 1);
  };

  return (
    <ScrollView style={styles.onboardingScrollView} contentContainerStyle={styles.scrollContent}>
      <Text style={styles.headerTitle}>DIGITAL WITNESS</Text>
      <Text style={styles.subTitle}>Setup Your Safety Profile</Text>
      <Text style={styles.progressText}>Step {currentStep} of 6</Text>

      {/* Step 1: Name & Phone */}
      {currentStep === 1 && (
        <>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>FULL NAME</Text>
            <TextInput style={styles.input} placeholder="John Doe" placeholderTextColor="#666" value={name} onChangeText={setName} />
          </View>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>PHONE NUMBER</Text>
            <TextInput style={styles.input} placeholder="555-0123" keyboardType="phone-pad" placeholderTextColor="#666" value={phone} onChangeText={setPhone} />
          </View>
        </>
      )}

      {/* Step 2: Demographics */}
      {currentStep === 2 && (
        <>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>GENDER</Text>
            <TextInput style={styles.input} placeholder="Male/Female/Other" placeholderTextColor="#666" value={gender} onChangeText={setGender} />
          </View>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>DATE OF BIRTH</Text>
            <TextInput style={styles.input} placeholder="MM/DD/YYYY" placeholderTextColor="#666" value={dateOfBirth} onChangeText={setDateOfBirth} />
          </View>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>CITIZENSHIP STATUS</Text>
            <TextInput style={styles.input} placeholder="Citizen, Permanent Resident, Visa" placeholderTextColor="#666" value={citizenship} onChangeText={setCitizenship} />
          </View>
        </>
      )}

      {/* Step 3: Emergency Contact */}
      {currentStep === 3 && (
        <>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>EMERGENCY CONTACT NAME</Text>
            <TextInput style={styles.input} placeholder="Jane Doe" placeholderTextColor="#666" value={emergencyName} onChangeText={setEmergencyName} />
          </View>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>EMERGENCY CONTACT PHONE</Text>
            <TextInput style={styles.input} placeholder="555-9876" keyboardType="phone-pad" placeholderTextColor="#666" value={emergencyPhone} onChangeText={setEmergencyPhone} />
          </View>
        </>
      )}

      {/* Step 4: Lawyer Info (Optional) */}
      {currentStep === 4 && (
        <>
          <Text style={styles.optionalLabel}>(Optional - Skip if you don't have a lawyer)</Text>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>LAWYER NAME</Text>
            <TextInput style={styles.input} placeholder="Attorney Name" placeholderTextColor="#666" value={lawyerName} onChangeText={setLawyerName} />
          </View>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>LAWYER PHONE</Text>
            <TextInput style={styles.input} placeholder="555-5555" keyboardType="phone-pad" placeholderTextColor="#666" value={lawyerPhone} onChangeText={setLawyerPhone} />
          </View>
        </>
      )}

      {/* Step 5: Permissions */}
      {currentStep === 5 && (
        <>
          <Text style={styles.permissionsTitle}>Grant Permissions:</Text>
          <View style={styles.permissionRow}>
            <Text style={styles.permissionLabel}>Camera</Text>
            <TouchableOpacity style={permCamera ? styles.toggleActive : styles.toggleInactive} onPress={() => setPermCamera(!permCamera)}>
              <Text style={styles.toggleText}>{permCamera ? 'ON' : 'OFF'}</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.permissionRow}>
            <Text style={styles.permissionLabel}>Microphone</Text>
            <TouchableOpacity style={permMic ? styles.toggleActive : styles.toggleInactive} onPress={() => setPermMic(!permMic)}>
              <Text style={styles.toggleText}>{permMic ? 'ON' : 'OFF'}</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.permissionRow}>
            <Text style={styles.permissionLabel}>Photos</Text>
            <TouchableOpacity style={permPhotos ? styles.toggleActive : styles.toggleInactive} onPress={() => setPermPhotos(!permPhotos)}>
              <Text style={styles.toggleText}>{permPhotos ? 'ON' : 'OFF'}</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.permissionRow}>
            <Text style={styles.permissionLabel}>Calls</Text>
            <TouchableOpacity style={permCalls ? styles.toggleActive : styles.toggleInactive} onPress={() => setPermCalls(!permCalls)}>
              <Text style={styles.toggleText}>{permCalls ? 'ON' : 'OFF'}</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.permissionRow}>
            <Text style={styles.permissionLabel}>Location</Text>
            <TouchableOpacity style={permLocation ? styles.toggleActive : styles.toggleInactive} onPress={() => setPermLocation(!permLocation)}>
              <Text style={styles.toggleText}>{permLocation ? 'ON' : 'OFF'}</Text>
            </TouchableOpacity>
          </View>
        </>
      )}

      {/* Step 6: PIN */}
      {currentStep === 6 && (
        <>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>CREATE SECURITY PIN (4-6 digits)</Text>
            <TextInput
              style={styles.input}
              placeholder="Enter PIN"
              placeholderTextColor="#666"
              value={pin}
              onChangeText={setPin}
              keyboardType="number-pad"
              secureTextEntry
              maxLength={6}
            />
          </View>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>CONFIRM PIN</Text>
            <TextInput
              style={styles.input}
              placeholder="Re-enter PIN"
              placeholderTextColor="#666"
              value={pinConfirm}
              onChangeText={setPinConfirm}
              keyboardType="number-pad"
              secureTextEntry
              maxLength={6}
            />
          </View>
        </>
      )}

      {/* Navigation Buttons */}
      <View style={styles.navigationButtons}>
        {currentStep > 1 && (
          <TouchableOpacity style={styles.secondaryButton} onPress={handlePrevious}>
            <Text style={styles.secondaryButtonText}>PREVIOUS</Text>
          </TouchableOpacity>
        )}
        <TouchableOpacity style={styles.primaryButton} onPress={handleNext}>
          <Text style={styles.buttonText}>{currentStep === 6 ? 'SAVE PROFILE' : 'NEXT'}</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}


// --- SUMMARY SCREEN ---
function SummaryScreen({ sessionDuration, onHome }) {
  const [loading, setLoading] = useState(false);
  const [report, setReport] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);

  const generateReport = async () => {
    console.log("📊 Generating report...");
    setLoading(true);
    setModalVisible(true);
    try {
      const body = {
        session_id: "123",
        logs: [
          { role: "user", content: "They are asking for ID." },
          { role: "ai", content: "Ask: Am I free to go?" },
          { role: "user", content: "Am I free to go?" },
          { role: "ai", content: "If no answer, remain silent." }
        ]
      };

      console.log("📤 Sending report request...");
      const response = await fetch('https://vglyohhcnhmjqqylpyss.supabase.co/functions/v1/generate-report', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${SUPABASE_KEY}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });

      console.log("📥 Report response:", response.status, response.statusText);

      if (!response.ok) {
        const errorText = await response.text();
        console.error("❌ Report error:", errorText);
        throw new Error(`Server returned ${response.status}: ${errorText}`);
      }

      const data = await response.json();
      console.log("✅ Report received, length:", data.report?.length || 0);
      setReport(data.report);

    } catch (e) {
      console.error("❌ Report generation failed:", e.message);
      setReport(`Failed to generate report: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.screenContainer}>
      <Ionicons name="checkmark-circle" size={80} color="#00FF00" style={{ marginBottom: 20 }} />
      <Text style={styles.headerTitle}>SESSION SECURED</Text>
      <Text style={styles.subTitle}>Evidence Uploaded to Cloud Vault</Text>

      <View style={styles.statsContainer}>
        <Text style={styles.statText}>DURATION: {Math.floor(sessionDuration / 60)}m {sessionDuration % 60}s</Text>
        <Text style={styles.statText}>DATE: {new Date().toLocaleDateString()}</Text>
        <Text style={styles.statText}>STATUS: ENCRYPTED</Text>
      </View>

      <TouchableOpacity style={styles.reportButton} onPress={generateReport}>
        <Text style={styles.reportButtonText}>GENERATE INCIDENT REPORT</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.historyButton}
        onPress={() => Alert.alert("Session History", "Last Event:\nICE Investigation\nDuration: 0:29\nLocation: Pittsburgh, PA\nDate: " + new Date().toLocaleDateString())}
      >
        <Ionicons name="time-outline" size={20} color="#FFF" />
        <Text style={styles.historyButtonText}>VIEW HISTORY</Text>
      </TouchableOpacity>

      <TouchableOpacity style={styles.primaryButton} onPress={onHome}>
        <Text style={styles.buttonText}>RETURN TO DASHBOARD</Text>
      </TouchableOpacity>

      {/* Report Modal */}
      <Modal visible={modalVisible} animationType="slide" presentationStyle="pageSheet">
        <View style={styles.modalOverlay}>
          <View style={styles.reportModalContent}>
            <Text style={styles.reportTitle}>INCIDENT REPORT</Text>
            {loading ? (
              <ActivityIndicator size="large" color="#00FF00" />
            ) : (
              <ScrollView style={styles.reportScroll}>
                <Text style={styles.reportText}>{report}</Text>
              </ScrollView>
            )}
            <TouchableOpacity style={styles.closeButton} onPress={() => setModalVisible(false)}>
              <Text style={styles.closeButtonText}>CLOSE REPORT</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </View>
  );
}

// ... styles continue ...

// --- STYLES ---
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  screenContainer: {
    flex: 1,
    backgroundColor: '#000',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  // Typography
  headerTitle: {
    fontSize: 32,
    fontWeight: '900',
    color: '#FFF',
    marginBottom: 10,
    letterSpacing: 1,
    textAlign: 'center',
  },
  subTitle: {
    fontSize: 18,
    color: '#AAA',
    marginBottom: 40,
    textAlign: 'center',
  },
  label: {
    color: '#FFF',
    fontWeight: 'bold',
    fontSize: 14,
    marginBottom: 5,
  },
  // Inputs
  inputGroup: {
    width: '100%',
    marginBottom: 20,
  },
  input: {
    backgroundColor: '#222',
    color: '#FFF',
    padding: 15,
    borderRadius: 8,
    fontSize: 16,
    borderWidth: 1,
    borderColor: '#444',
  },
  // Buttons
  primaryButton: {
    backgroundColor: '#0066CC',
    paddingVertical: 18,
    paddingHorizontal: 40,
    borderRadius: 12,
    marginTop: 20,
    width: '100%',
    alignItems: 'center',
  },
  buttonText: {
    color: '#FFF',
    fontSize: 18,
    fontWeight: 'bold',
  },
  // Dashboard Components
  topBar: {
    position: 'absolute',
    top: 60,
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusText: {
    color: '#00FF00',
    marginLeft: 10,
    fontSize: 16,
    fontWeight: 'bold',
    letterSpacing: 1,
  },
  centerContent: {
    alignItems: 'center',
  },
  armButton: {
    width: 250,
    height: 250,
    borderRadius: 125,
    backgroundColor: '#330000',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 4,
    borderColor: '#FF0000',
    marginBottom: 30,
    shadowColor: "#FF0000",
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.5,
    shadowRadius: 20,
    elevation: 10,
  },
  armInner: {
    width: 220,
    height: 220,
    borderRadius: 110,
    backgroundColor: '#CC0000',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#FF4444',
  },
  armText: {
    color: '#FFF',
    fontSize: 48,
    fontWeight: '900',
  },
  hintText: {
    color: '#666',
    fontSize: 14,
    letterSpacing: 2,
  },
  footer: {
    position: 'absolute',
    bottom: 40,
  },
  footerText: {
    color: '#444',
    fontSize: 12,
  },
  // Active Session
  overlayContainer: {
    flex: 1,
    justifyContent: 'space-between',
    paddingTop: 50,
    paddingBottom: 30,
    paddingHorizontal: 20,
  },
  activeTopBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.5)',
    padding: 10,
    borderRadius: 8,
  },
  recIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  redDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#FF0000',
    marginRight: 6,
  },
  recText: {
    color: '#FFF',
    fontWeight: 'bold',
  },
  timerText: {
    color: '#FFF',
    fontSize: 20,
    fontVariant: ['tabular-nums'],
  },
  uploadIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  uploadText: {
    color: '#00FFFF',
    fontWeight: 'bold',
    fontSize: 12,
  },
  aiHud: {
    alignItems: 'center',
    marginTop: 20,
  },
  visionText: {
    color: '#00FF00',
    fontSize: 12,
    marginBottom: 8,
    backgroundColor: 'rgba(0,0,0,0.6)',
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  rightsBanner: {
    backgroundColor: 'rgba(255,0,0,0.8)',
    paddingVertical: 15,
    paddingHorizontal: 10,
    width: '110%',
    alignItems: 'center',
  },
  rightsText: {
    color: '#FFF',
    fontSize: 24,
    fontWeight: '900',
    textAlign: 'center',
    textTransform: 'uppercase',
  },
  aiStatusContainer: {
    alignItems: 'center',
    marginVertical: 10,
  },
  aiStatusText: {
    color: '#00FFFF',
    fontSize: 18,
    fontWeight: 'bold',
    textAlign: 'center',
    textShadowColor: 'black',
    textShadowRadius: 5,
  },
  aiStatusActive: {
    color: '#FF00FF',
    fontSize: 22,
  },
  activeFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
  },
  deescalateButton: {
    backgroundColor: '#FFFF00',
    width: 70,
    height: 70,
    borderRadius: 35,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#FFF',
  },
  deescalateText: {
    color: '#000',
    fontSize: 9,
    fontWeight: 'bold',
    marginTop: 2,
    textAlign: 'center'
  },
  talkButton: {
    backgroundColor: 'rgba(0,0,255,0.6)',
    width: 100,
    height: 100,
    borderRadius: 50,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#00FFFF',
    marginBottom: 10,
  },
  talkButtonActive: {
    backgroundColor: 'rgba(255,0,255,0.8)',
    borderColor: '#FFF',
    transform: [{ scale: 1.1 }],
  },
  talkButtonText: {
    color: '#FFF',
    fontSize: 10,
    fontWeight: 'bold',
    marginTop: 5,
    textAlign: 'center',
  },
  stopButton: {
    backgroundColor: '#330000',
    width: 70,
    height: 70,
    borderRadius: 35,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#FF0000',
  },
  stopInner: {
    width: 20,
    height: 20,
    backgroundColor: '#FF0000',
    borderRadius: 2,
    marginBottom: 2,
  },
  stopText: {
    color: '#FF0000',
    fontSize: 12,
    fontWeight: 'bold',
  },
  lawyerButtonFloating: {
    position: 'absolute',
    top: 50, // Moved to TOP
    left: 20, // Moved to LEFT to avoid Stop button overlap
    backgroundColor: '#0066CC',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 30,
    flexDirection: 'row',
    alignItems: 'center',
    elevation: 5,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 3,
  },
  lawyerButtonText: {
    color: 'white',
    fontWeight: 'bold',
    marginLeft: 5,
    fontSize: 14,
  },
  // Modal
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.9)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalContent: {
    width: '80%',
    backgroundColor: '#222',
    padding: 30,
    borderRadius: 20,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#444',
  },
  modalTitle: {
    color: '#FFF',
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  pinInput: {
    backgroundColor: '#000',
    color: '#FFF',
    width: '100%',
    fontSize: 32,
    fontWeight: 'bold',
    textAlign: 'center',
    padding: 15,
    borderRadius: 10,
    marginBottom: 20,
    letterSpacing: 10,
    borderWidth: 1,
    borderColor: '#666',
  },
  cancelButton: {
    padding: 10,
  },
  cancelText: {
    color: '#AAA',
    fontSize: 16,
  },
  // Summary
  statsContainer: {
    backgroundColor: '#111',
    padding: 20,
    borderRadius: 10,
    width: '100%',
    marginVertical: 20,
  },
  statText: {
    color: '#FFF',
    fontSize: 16,
    marginBottom: 10,
    fontFamily: 'monospace',
  },
  reportButton: {
    backgroundColor: '#333',
    paddingVertical: 15,
    paddingHorizontal: 30,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#00FF00',
    marginTop: 10,
    width: '100%',
    alignItems: 'center',
  },
  reportButtonText: {
    color: '#00FF00',
    fontWeight: 'bold',
    fontSize: 14,
  },
  reportModalContent: {
    flex: 1,
    backgroundColor: '#111',
    padding: 20,
    marginTop: 50,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
  },
  reportTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFF',
    marginBottom: 20,
    textAlign: 'center',
    borderBottomWidth: 1,
    borderBottomColor: '#333',
    paddingBottom: 10,
  },
  reportScroll: {
    flex: 1,
  },
  reportText: {
    color: '#EEE',
    fontSize: 16,
    lineHeight: 24,
  },
  closeButton: {
    backgroundColor: '#CC0000',
    padding: 15,
    borderRadius: 10,
    marginTop: 20,
    alignItems: 'center',
    marginBottom: 20,
  },
  closeButtonText: {
    color: '#FFF',
    fontWeight: 'bold',
  },
  // Onboarding Multi-Step Styles
  onboardingScrollView: {
    flex: 1,
    backgroundColor: '#000',
    width: '100%',
  },
  scrollContent: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 40,
    paddingHorizontal: 20,
    width: '100%',
  },
  progressText: {
    color: '#00BFFF',
    fontSize: 14,
    marginBottom: 30,
    fontWeight: 'bold',
  },
  optionalLabel: {
    color: '#888',
    fontSize: 13,
    marginBottom: 20,
    fontStyle: 'italic',
    textAlign: 'center',
  },
  navigationButtons: {
    flexDirection: 'row',
    width: '100%',
    justifyContent: 'space-between',
    marginTop: 20,
    gap: 10,
  },
  secondaryButton: {
    backgroundColor: '#333',
    paddingVertical: 18,
    paddingHorizontal: 30,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#666',
    flex: 1,
    alignItems: 'center',
  },
  secondaryButtonText: {
    color: '#AAA',
    fontWeight: 'bold',
    fontSize: 16,
  },
  // Permissions Styles
  permissionsTitle: {
    color: '#FFF',
    fontSize: 18,
    marginBottom: 20,
    fontWeight: 'bold',
  },
  permissionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    marginBottom: 15,
    paddingVertical: 10,
    paddingHorizontal: 15,
    backgroundColor: '#1A1A1A',
    borderRadius: 8,
  },
  permissionLabel: {
    color: '#FFF',
    fontSize: 16,
  },
  toggleActive: {
    backgroundColor: '#00FF00',
    paddingVertical: 8,
    paddingHorizontal: 20,
    borderRadius: 20,
  },
  toggleInactive: {
    backgroundColor: '#444',
    paddingVertical: 8,
    paddingHorizontal: 20,
    borderRadius: 20,
  },
  toggleText: {
    color: '#000',
    fontWeight: 'bold',
    fontSize: 12,
  },
  // Edit Profile Button
  editProfileButton: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 10,
    paddingVertical: 8,
    paddingHorizontal: 15,
    backgroundColor: '#1A1A1A',
    borderRadius: 8,
    gap: 5,
  },
  editProfileText: {
    color: '#00BFFF',
    fontSize: 14,
    fontWeight: 'bold',
  },
  // History Button
  historyButton: {
    backgroundColor: '#444',
    paddingVertical: 15,
    paddingHorizontal: 30,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#888',
    marginTop: 10,
    width: '100%',
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 8,
  },
  historyButtonText: {
    color: '#FFF',
    fontWeight: 'bold',
    fontSize: 14,
  },
});
