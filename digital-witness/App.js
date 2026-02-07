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
      setCurrentScreen('dashboard');
    } catch (e) {
      Alert.alert('Error', 'Failed to save profile');
    }
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

  const resetProfile = async () => {
    try {
      await AsyncStorage.removeItem(STORAGE_KEY_USER);
      setUserProfile(null);
      setCurrentScreen('onboarding');
    } catch (e) {
      Alert.alert('Error', 'Failed to reset profile');
    }
  };

  if (!cameraPermission || !micPermission) {
    // Permissions are still loading
    return <View style={styles.container} />;
  }

  return (
    <View style={styles.container}>
      <StatusBar style="light" />
      {currentScreen === 'loading' && <ActivityIndicator size="large" color="#FF0000" />}
      {currentScreen === 'onboarding' && <OnboardingScreen onSave={saveProfile} />}
      {currentScreen === 'dashboard' && <DashboardScreen user={userProfile} onArm={startSession} onResetProfile={resetProfile} />}
      {currentScreen === 'active' && <ActiveSessionScreen onEnd={endSession} recordingTime={recordingTime} setRecordingTime={setRecordingTime} />}
      {currentScreen === 'summary' && <SummaryScreen sessionDuration={recordingTime} onHome={() => setCurrentScreen('dashboard')} />}
    </View>
  );
}

// --- DASHBOARD SCREEN ---
function DashboardScreen({ user, onArm, onResetProfile }) {
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
        <Text style={styles.footerText}>User: {user?.name} | {user?.status}</Text>
        <TouchableOpacity style={styles.resetProfileButton} onPress={onResetProfile}>
          <Ionicons name="person-circle-outline" size={16} color="#888" />
          <Text style={styles.resetProfileText}>Edit Profile</Text>
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
  const [aiStatus, setAiStatus] = useState("AI ADVISOR ACTIVE");
  const [aiVisionText, setAiVisionText] = useState("Analyzing Scene...");
  const [lawyerNotified, setLawyerNotified] = useState(false);
  const [nextAdviceIn, setNextAdviceIn] = useState(15);
  const [conversationHistory, setConversationHistory] = useState([]);
  const recordingRef = useRef(null);
  const [isRecording, setIsRecording] = useState(false);

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

  // Lawyer notification effect - show for 10 seconds
  useEffect(() => {
    const showTimer = setTimeout(() => {
      setLawyerNotified(true);
    }, 2000); // Show lawyer notification after 2 seconds

    const hideTimer = setTimeout(() => {
      setLawyerNotified(false);
    }, 12000); // Hide after 10 seconds (2s + 10s = 12s total)

    return () => {
      clearTimeout(showTimer);
      clearTimeout(hideTimer);
    };
  }, []);

  // Start continuous recording
  useEffect(() => {
    const startRecording = async () => {
      try {
        console.log("🎙️ Starting continuous recording...");
        await Audio.setAudioModeAsync({
          allowsRecordingIOS: true,
          playsInSilentModeIOS: true
        });
        const { recording } = await Audio.Recording.createAsync(
          Audio.RecordingOptionsPresets.HIGH_QUALITY
        );
        recordingRef.current = recording;
        setIsRecording(true);
        console.log("✅ Continuous recording started");
      } catch (err) {
        console.error("❌ Recording error:", err);
        setAiStatus("MIC ERROR");
      }
    };

    startRecording();

    return () => {
      if (recordingRef.current) {
        recordingRef.current.stopAndUnloadAsync();
      }
    };
  }, []);

  // Countdown timer
  useEffect(() => {
    const countdownInterval = setInterval(() => {
      setNextAdviceIn(prev => {
        if (prev <= 1) {
          return 15; // Reset to 15
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(countdownInterval);
  }, []);

  // Send audio to server every 15 seconds
  useEffect(() => {
    const sendAudioForAdvice = async () => {
      if (!recordingRef.current || !isRecording) {
        console.log("⚠️ No active recording");
        return;
      }

      try {
        console.log("📤 Sending audio chunk to AI advisor...");
        setAiStatus("ANALYZING...");

        // Stop current recording
        const recording = recordingRef.current;
        await recording.stopAndUnloadAsync();
        const uri = recording.getURI();

        // Prepare form data with audio and conversation history
        const formData = new FormData();
        formData.append('audio', {
          uri: uri,
          type: 'audio/m4a',
          name: 'voice.m4a',
        });

        // Add conversation history as context
        if (conversationHistory.length > 0) {
          formData.append('context', JSON.stringify(conversationHistory));
        }

        // Send to server
        const response = await fetch(SUPABASE_URL, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${SUPABASE_KEY}`,
          },
          body: formData,
        });

        if (!response.ok) {
          const errorText = await response.text();
          console.error("❌ Server error:", errorText);
          setAiStatus("SERVER ERROR");
        } else {
          const data = await response.json();
          console.log("✅ AI Response:", data);

          if (data.advice) {
            setAiStatus(`ADVICE: ${data.advice}`);

            // Speak the advice
            Speech.speak(data.advice, {
              language: 'en',
              pitch: 1.0,
              rate: 1.0,
            });

            // Update conversation history with transcript and advice
            setConversationHistory(prev => [
              ...prev,
              {
                timestamp: Date.now(),
                transcript: data.transcript || "",
                advice: data.advice
              }
            ]);
          } else {
            setAiStatus("AI ADVISOR ACTIVE");
          }
        }

        // Restart recording for next interval
        const { recording: newRecording } = await Audio.Recording.createAsync(
          Audio.RecordingOptionsPresets.HIGH_QUALITY
        );
        recordingRef.current = newRecording;

      } catch (error) {
        console.error("❌ Error:", error);
        setAiStatus("ERROR - RETRYING");

        // Try to restart recording
        try {
          const { recording: newRecording } = await Audio.Recording.createAsync(
            Audio.RecordingOptionsPresets.HIGH_QUALITY
          );
          recordingRef.current = newRecording;
        } catch (restartErr) {
          console.error("❌ Failed to restart recording:", restartErr);
        }
      }
    };

    const advisorInterval = setInterval(() => {
      sendAudioForAdvice();
    }, 15000); // Every 15 seconds

    return () => clearInterval(advisorInterval);
  }, [isRecording, conversationHistory]);


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

        {/* Lawyer Notification - Very top */}
        {lawyerNotified && (
          <View style={styles.lawyerNotificationTop}>
            <Ionicons name="checkmark-circle" size={14} color="#FFFFFF" />
            <Text style={styles.lawyerNotificationTopText}>Notifying the registered lawyer.</Text>
          </View>
        )}

        {/* AI Advice Banner - High on screen */}
        <View style={styles.adviceBanner}>
          <Text style={styles.adviceText}>{aiStatus}</Text>
          <Text style={styles.adviceTimerText}>Next: {nextAdviceIn}s</Text>
        </View>

        {/* Vision Analysis - Below advice */}
        <View style={styles.visionContainer}>
          <Text style={styles.visionText}>{aiVisionText}</Text>
        </View>


        {/* Bottom Controls */}
        <View style={styles.activeFooter}>
          <View style={{ flex: 1 }} />

          <View style={{ alignItems: 'center' }}>
            <TouchableOpacity style={styles.stopButton} onPress={() => { setModalVisible(true); setPin(''); }}>
              <View style={styles.stopInner} />
              <Text style={styles.stopText}>STOP</Text>
            </TouchableOpacity>
          </View>

          <View style={{ flex: 1 }} />
        </View>


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

// --- ONBOARDING SCREEN ---
function OnboardingScreen({ onSave }) {
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState({
    name: '',
    phone: '',
    dateOfBirth: '',
    gender: '',
    citizenshipStatus: '',
    emergencyContactName: '',
    emergencyContactPhone: '',
    lawyerName: '',
    lawyerPhone: '',
    lawyerOrganization: '',
    pin: '',
    confirmPin: '',
  });
  const [permissions, setPermissions] = useState({
    camera: false,
    microphone: false,
    contacts: false,
    location: false,
  });

  const updateFormData = (key, value) => {
    setFormData(prev => ({ ...prev, [key]: value }));
  };

  const handleNext = (requiredFields) => {
    // Validate required fields for current step
    const missingFields = requiredFields.filter(field => !formData[field]);
    if (missingFields.length > 0) {
      Alert.alert('Missing Information', 'Please fill in all required fields.');
      return;
    }
    setCurrentStep(prev => prev + 1);
  };

  const handleSave = () => {
    // Validate final step
    if (!formData.emergencyContactName || !formData.emergencyContactPhone) {
      Alert.alert('Missing Information', 'Please provide emergency contact information for your safety.');
      return;
    }

    // Save the complete profile
    onSave({
      name: formData.name,
      phone: formData.phone,
      dateOfBirth: formData.dateOfBirth,
      gender: formData.gender,
      status: formData.citizenshipStatus, // Keep 'status' for backward compatibility
      contact: formData.emergencyContactPhone, // Keep 'contact' for backward compatibility
      emergencyContactName: formData.emergencyContactName,
      emergencyContactPhone: formData.emergencyContactPhone,
    });
  };

  return (
    <View style={styles.onboardingContainer}>
      {/* Header */}
      <View style={styles.onboardingHeader}>
        <Text style={styles.onboardingTitle}>Digital Witness</Text>
        <Text style={styles.onboardingSubtitle}>Safety Profile Setup</Text>
      </View>

      {/* Step Indicators */}
      <View style={styles.stepIndicator}>
        <View style={[styles.stepDot, currentStep >= 1 && styles.stepDotActive]}>
          <Text style={styles.stepDotText}>1</Text>
        </View>
        <View style={styles.stepLine} />
        <View style={[styles.stepDot, currentStep >= 2 && styles.stepDotActive]}>
          <Text style={styles.stepDotText}>2</Text>
        </View>
        <View style={styles.stepLine} />
        <View style={[styles.stepDot, currentStep >= 3 && styles.stepDotActive]}>
          <Text style={styles.stepDotText}>3</Text>
        </View>
        <View style={styles.stepLine} />
        <View style={[styles.stepDot, currentStep >= 4 && styles.stepDotActive]}>
          <Text style={styles.stepDotText}>4</Text>
        </View>
        <View style={styles.stepLine} />
        <View style={[styles.stepDot, currentStep >= 5 && styles.stepDotActive]}>
          <Text style={styles.stepDotText}>5</Text>
        </View>
        <View style={styles.stepLine} />
        <View style={[styles.stepDot, currentStep >= 6 && styles.stepDotActive]}>
          <Text style={styles.stepDotText}>6</Text>
        </View>
      </View>

      {/* Content Area */}
      <ScrollView style={styles.onboardingContent} contentContainerStyle={styles.onboardingContentContainer}>
        {currentStep === 1 && (
          <View style={styles.questionnaireStep}>
            <Text style={styles.stepTitle}>Personal Information</Text>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Full Name <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="John Doe"
                placeholderTextColor="#999"
                value={formData.name}
                onChangeText={(val) => updateFormData('name', val)}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Phone Number <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="(555) 123-4567"
                placeholderTextColor="#999"
                keyboardType="phone-pad"
                value={formData.phone}
                onChangeText={(val) => updateFormData('phone', val)}
              />
            </View>

            <TouchableOpacity
              style={styles.nextButton}
              onPress={() => handleNext(['name', 'phone'])}
            >
              <Text style={styles.nextButtonText}>Next</Text>
            </TouchableOpacity>
          </View>
        )}

        {currentStep === 2 && (
          <View style={styles.questionnaireStep}>
            <Text style={styles.stepTitle}>Additional Details</Text>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Date of Birth <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="MM/DD/YYYY"
                placeholderTextColor="#999"
                value={formData.dateOfBirth}
                onChangeText={(val) => updateFormData('dateOfBirth', val)}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Gender <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="Male / Female / Other"
                placeholderTextColor="#999"
                value={formData.gender}
                onChangeText={(val) => updateFormData('gender', val)}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Citizenship Status <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="e.g., U.S. Citizen, F-1 Visa, Green Card"
                placeholderTextColor="#999"
                value={formData.citizenshipStatus}
                onChangeText={(val) => updateFormData('citizenshipStatus', val)}
              />
            </View>

            <TouchableOpacity
              style={styles.nextButton}
              onPress={() => handleNext(['dateOfBirth', 'gender', 'citizenshipStatus'])}
            >
              <Text style={styles.nextButtonText}>Next</Text>
            </TouchableOpacity>
          </View>
        )}

        {currentStep === 3 && (
          <View style={styles.questionnaireStep}>
            <Text style={styles.stepTitle}>Emergency Contact</Text>
            <Text style={styles.stepDescription}>
              This person will be contacted in case of an emergency.
            </Text>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Contact Name <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="Jane Doe"
                placeholderTextColor="#999"
                value={formData.emergencyContactName}
                onChangeText={(val) => updateFormData('emergencyContactName', val)}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Contact Phone <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="(555) 987-6543"
                placeholderTextColor="#999"
                keyboardType="phone-pad"
                value={formData.emergencyContactPhone}
                onChangeText={(val) => updateFormData('emergencyContactPhone', val)}
              />
            </View>

            <TouchableOpacity
              style={styles.nextButton}
              onPress={() => handleNext(['emergencyContactName', 'emergencyContactPhone'])}
            >
              <Text style={styles.nextButtonText}>Next</Text>
            </TouchableOpacity>
          </View>
        )}

        {currentStep === 4 && (
          <View style={styles.questionnaireStep}>
            <Text style={styles.stepTitle}>Lawyer Details</Text>
            <Text style={styles.stepDescription}>
              Optional: Add your attorney's information for quick access.
            </Text>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Lawyer Name</Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="John Smith, Esq."
                placeholderTextColor="#999"
                value={formData.lawyerName}
                onChangeText={(val) => updateFormData('lawyerName', val)}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Lawyer Phone</Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="(555) 444-3333"
                placeholderTextColor="#999"
                keyboardType="phone-pad"
                value={formData.lawyerPhone}
                onChangeText={(val) => updateFormData('lawyerPhone', val)}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Law Firm / Organization</Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="Legal Aid Society"
                placeholderTextColor="#999"
                value={formData.lawyerOrganization}
                onChangeText={(val) => updateFormData('lawyerOrganization', val)}
              />
            </View>

            <TouchableOpacity
              style={styles.nextButton}
              onPress={() => setCurrentStep(5)}
            >
              <Text style={styles.nextButtonText}>Next</Text>
            </TouchableOpacity>
          </View>
        )}

        {currentStep === 5 && (
          <View style={styles.questionnaireStep}>
            <Text style={styles.stepTitle}>Permissions</Text>
            <Text style={styles.stepDescription}>
              Grant necessary permissions for full app functionality.
            </Text>

            <TouchableOpacity
              style={styles.permissionItem}
              onPress={() => setPermissions(prev => ({ ...prev, camera: !prev.camera }))}
            >
              <View style={styles.permissionLeft}>
                <Ionicons name="camera" size={24} color="#2196F3" />
                <View style={styles.permissionText}>
                  <Text style={styles.permissionTitle}>Camera</Text>
                  <Text style={styles.permissionDesc}>Record video evidence</Text>
                </View>
              </View>
              <View style={[styles.toggle, permissions.camera && styles.toggleActive]}>
                <View style={[styles.toggleThumb, permissions.camera && styles.toggleThumbActive]} />
              </View>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.permissionItem}
              onPress={() => setPermissions(prev => ({ ...prev, microphone: !prev.microphone }))}
            >
              <View style={styles.permissionLeft}>
                <Ionicons name="mic" size={24} color="#2196F3" />
                <View style={styles.permissionText}>
                  <Text style={styles.permissionTitle}>Microphone</Text>
                  <Text style={styles.permissionDesc}>Record audio and ask AI advisor</Text>
                </View>
              </View>
              <View style={[styles.toggle, permissions.microphone && styles.toggleActive]}>
                <View style={[styles.toggleThumb, permissions.microphone && styles.toggleThumbActive]} />
              </View>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.permissionItem}
              onPress={() => setPermissions(prev => ({ ...prev, contacts: !prev.contacts }))}
            >
              <View style={styles.permissionLeft}>
                <Ionicons name="call" size={24} color="#2196F3" />
                <View style={styles.permissionText}>
                  <Text style={styles.permissionTitle}>Calls & Messages</Text>
                  <Text style={styles.permissionDesc}>Contact emergency contacts</Text>
                </View>
              </View>
              <View style={[styles.toggle, permissions.contacts && styles.toggleActive]}>
                <View style={[styles.toggleThumb, permissions.contacts && styles.toggleThumbActive]} />
              </View>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.permissionItem}
              onPress={() => setPermissions(prev => ({ ...prev, location: !prev.location }))}
            >
              <View style={styles.permissionLeft}>
                <Ionicons name="location" size={24} color="#2196F3" />
                <View style={styles.permissionText}>
                  <Text style={styles.permissionTitle}>Location</Text>
                  <Text style={styles.permissionDesc}>Log incident location</Text>
                </View>
              </View>
              <View style={[styles.toggle, permissions.location && styles.toggleActive]}>
                <View style={[styles.toggleThumb, permissions.location && styles.toggleThumbActive]} />
              </View>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.nextButton}
              onPress={() => setCurrentStep(6)}
            >
              <Text style={styles.nextButtonText}>Next</Text>
            </TouchableOpacity>
          </View>
        )}

        {currentStep === 6 && (
          <View style={styles.questionnaireStep}>
            <Text style={styles.stepTitle}>Create a Secure PIN</Text>
            <Text style={styles.stepDescription}>
              Create a 4-digit PIN to stop recording sessions.
            </Text>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Enter PIN <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="••••"
                placeholderTextColor="#999"
                keyboardType="numeric"
                maxLength={4}
                secureTextEntry
                value={formData.pin}
                onChangeText={(val) => updateFormData('pin', val)}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Confirm PIN <Text style={styles.required}>*</Text></Text>
              <TextInput
                style={styles.onboardingInput}
                placeholder="••••"
                placeholderTextColor="#999"
                keyboardType="numeric"
                maxLength={4}
                secureTextEntry
                value={formData.confirmPin}
                onChangeText={(val) => updateFormData('confirmPin', val)}
              />
            </View>

            <TouchableOpacity
              style={styles.saveButton}
              onPress={handleSave}
            >
              <Text style={styles.saveButtonText}>Finish Setup</Text>
            </TouchableOpacity>
          </View>
        )}
      </ScrollView>
    </View>
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

      <TouchableOpacity style={styles.historyButton} onPress={() => Alert.alert("History", "ICE investigation, Record time 0:29, Pittsburgh, PA")}>
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
    marginBottom: 8,
  },
  resetProfileButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: '#222',
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#444',
  },
  resetProfileText: {
    color: '#888',
    fontSize: 12,
    marginLeft: 6,
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
  aiTimerText: {
    color: '#00FFFF',
    fontSize: 14,
    marginTop: 5,
    textAlign: 'center',
  },
  lawyerNotification: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(76, 175, 80, 0.2)',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    marginHorizontal: 20,
    marginTop: 10,
    borderWidth: 1,
    borderColor: '#4CAF50',
  },
  lawyerNotificationText: {
    color: '#4CAF50',
    fontSize: 14,
    fontWeight: '600',
    marginLeft: 8,
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
  },
  historyButtonText: {
    color: '#FFF',
    fontWeight: 'bold',
    fontSize: 14,
    marginLeft: 5,
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
  // Onboarding Styles
  onboardingContainer: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  onboardingHeader: {
    backgroundColor: '#2196F3',
    paddingTop: 60,
    paddingBottom: 30,
    paddingHorizontal: 20,
    alignItems: 'center',
  },
  onboardingTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#FFFFFF',
    marginBottom: 5,
  },
  onboardingSubtitle: {
    fontSize: 16,
    color: '#E3F2FD',
  },
  stepIndicator: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 30,
    backgroundColor: '#FFFFFF',
  },
  stepDot: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#E0E0E0',
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepDotActive: {
    backgroundColor: '#2196F3',
  },
  stepDotText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontSize: 16,
  },
  stepLine: {
    width: 30,
    height: 2,
    backgroundColor: '#E0E0E0',
  },
  onboardingContent: {
    flex: 1,
  },
  onboardingContentContainer: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  questionnaireStep: {
    width: '100%',
  },
  stepTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#212121',
    marginBottom: 10,
  },
  stepDescription: {
    fontSize: 14,
    color: '#757575',
    marginBottom: 20,
  },
  onboardingInput: {
    backgroundColor: '#F5F5F5',
    color: '#212121',
    padding: 15,
    borderRadius: 8,
    fontSize: 16,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  required: {
    color: '#F44336',
  },
  nextButton: {
    backgroundColor: '#2196F3',
    paddingVertical: 16,
    borderRadius: 8,
    marginTop: 30,
    alignItems: 'center',
    shadowColor: '#2196F3',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 3,
  },
  nextButtonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 'bold',
  },
  saveButton: {
    backgroundColor: '#4CAF50',
    paddingVertical: 16,
    borderRadius: 8,
    marginTop: 30,
    alignItems: 'center',
    shadowColor: '#4CAF50',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 3,
  },
  saveButtonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 'bold',
  },
  // Permission Items
  permissionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
    padding: 16,
    borderRadius: 8,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  permissionLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  permissionText: {
    marginLeft: 12,
    flex: 1,
  },
  permissionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#212121',
    marginBottom: 2,
  },
  permissionDesc: {
    fontSize: 12,
    color: '#757575',
  },
  toggle: {
    width: 50,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#BDBDBD',
    padding: 2,
    justifyContent: 'center',
  },
  toggleActive: {
    backgroundColor: '#2196F3',
  },
  toggleThumb: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#FFFFFF',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
    elevation: 2,
  },
  toggleThumbActive: {
    transform: [{ translateX: 22 }],
  },
  lawyerNotificationTop: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#4CAF50',
    paddingVertical: 8,
    paddingHorizontal: 12,
    marginHorizontal: 10,
    marginTop: 5,
    borderRadius: 5,
    borderWidth: 2,
    borderColor: '#FFFFFF',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 5,
  },
  lawyerNotificationTopText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '600',
    marginLeft: 6,
  },
  adviceBanner: {
    backgroundColor: 'rgba(255, 0, 0, 0.6)',
    paddingVertical: 12,
    paddingHorizontal: 15,
    borderRadius: 5,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  adviceText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: 'bold',
    flex: 1,
  },
  adviceTimerText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '600',
    marginLeft: 10,
  },
  visionContainer: {
    marginTop: 10,
    marginHorizontal: 15,
  },
  recordingIndicator: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 60,
  },
  recordingText: {
    color: '#FF0000',
    fontSize: 10,
    fontWeight: 'bold',
    marginTop: 2,
  },
});
