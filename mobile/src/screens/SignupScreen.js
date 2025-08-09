import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator } from 'react-native';
import { useAuth } from '../hooks/useAuth';

export default function SignupScreen({ navigation }) {
  const { signup, loading } = useAuth();
  const [username, setUsername] = useState('');
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const onSubmit = async () => {
    setError('');
    try {
      await signup(username.trim(), userId.trim(), password);
    } catch (e) {
      setError('Signup failed');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Create Account</Text>
      {!!error && <Text style={styles.error}>{error}</Text>}
      <TextInput placeholder="Username" placeholderTextColor="#999" style={styles.input} value={username} onChangeText={setUsername} />
      <TextInput placeholder="User ID" placeholderTextColor="#999" style={styles.input} autoCapitalize="none" value={userId} onChangeText={setUserId} />
      <TextInput placeholder="Password" placeholderTextColor="#999" style={styles.input} secureTextEntry value={password} onChangeText={setPassword} />
      <TouchableOpacity style={styles.button} onPress={onSubmit} disabled={loading}>
        {loading ? <ActivityIndicator color="#000" /> : <Text style={styles.buttonText}>Sign Up</Text>}
      </TouchableOpacity>
      <TouchableOpacity onPress={() => navigation.goBack()}>
        <Text style={styles.link}>Back to login</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, backgroundColor: '#0b0b0b', justifyContent: 'center' },
  title: { color: '#fff', fontSize: 28, fontWeight: '700', marginBottom: 16, textAlign: 'center' },
  input: { backgroundColor: '#1b1b1b', color: '#fff', padding: 14, borderRadius: 8, marginBottom: 12 },
  button: { backgroundColor: '#1DB954', padding: 14, borderRadius: 8, alignItems: 'center', marginTop: 4 },
  buttonText: { color: '#000', fontWeight: '700' },
  link: { color: '#1DB954', textAlign: 'center', marginTop: 16 },
  error: { color: '#ff6b6b', marginBottom: 8, textAlign: 'center' }
});