import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  ScrollView,
  SafeAreaView,
  Dimensions,
  Platform,
} from 'react-native';
import { useVideoPlayer, VideoView } from 'expo-video';
import { StatusBar } from 'expo-status-bar';

interface VideoItem {
  id: string;
  title: string;
  category: string;
  duration: string;
  quality: string;
  size: string;
  description: string;
  url: string;
}

const SAMPLE_PLAYLIST: VideoItem[] = [
  {
    id: '1',
    title: 'Big Buck Bunny (Animation Demo)',
    category: 'Animation / Open Movie',
    duration: '09:56',
    quality: '1080p 60fps',
    size: '128 MB',
    description: 'Blender Foundation open animation movie sample with HDR master and spatial audio.',
    url: 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4',
  },
  {
    id: '2',
    title: 'Jellyfish Deep Ocean Wildlife',
    category: 'Nature / Wildlife',
    duration: '04:12',
    quality: '4K Ultra HD',
    size: '95 MB',
    description: 'Crisp underwater deep sea glowing jellyfish footage with smooth high bitrate playback.',
    url: 'https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4',
  },
  {
    id: '3',
    title: 'Sintel Fantasy Film Teaser',
    category: 'Cinematic / Sci-Fi',
    duration: '15:20',
    quality: '2K QHD',
    size: '210 MB',
    description: 'Blender Institute fantasy drama sample showcasing high dynamic range & color grade.',
    url: 'https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4',
  },
];

export default function App() {
  const [selectedVideo, setSelectedVideo] = useState<VideoItem>(SAMPLE_PLAYLIST[0]);
  const [activeTab, setActiveTab] = useState<'videos' | 'folders' | 'vault' | 'settings'>('videos');
  const [playbackSpeed, setPlaybackSpeed] = useState<'1.0x' | '1.25x' | '1.5x' | '2.0x'>('1.0x');
  const [isMuted, setIsMuted] = useState(true);
  const [aspectRatioMode, setAspectRatioMode] = useState<'Fit' | '16:9' | 'Fill'>('Fit');

  const player = useVideoPlayer(selectedVideo.url, (p) => {
    p.loop = true;
    p.muted = true;
    p.play();
  });

  const handleSelectVideo = (video: VideoItem) => {
    setSelectedVideo(video);
    player.replace(video.url);
    player.play();
  };

  const toggleMute = () => {
    const nextMuted = !isMuted;
    player.muted = nextMuted;
    setIsMuted(nextMuted);
  };

  const cycleSpeed = () => {
    const speeds: ('1.0x' | '1.25x' | '1.5x' | '2.0x')[] = ['1.0x', '1.25x', '1.5x', '2.0x'];
    const nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.length;
    const nextSpeed = speeds[nextIdx];
    setPlaybackSpeed(nextSpeed);
    const speedVal = parseFloat(nextSpeed.replace('x', ''));
    player.playbackRate = speedVal;
  };

  const cycleAspect = () => {
    const modes: ('Fit' | '16:9' | 'Fill')[] = ['Fit', '16:9', 'Fill'];
    const nextIdx = (modes.indexOf(aspectRatioMode) + 1) % modes.length;
    setAspectRatioMode(modes[nextIdx]);
  };

  const contentFitValue = aspectRatioMode === 'Fill' ? 'cover' : 'contain';

  return (
    <View style={styles.outerContainer}>
      {/* Device Shell (Simulating real phone frame on Web) */}
      <View style={styles.phoneFrame}>
        {/* Notch / Dynamic Island */}
        <View style={styles.notchContainer}>
          <View style={styles.dynamicIsland}>
            <View style={styles.cameraHole} />
            <View style={styles.speakerPill} />
          </View>
        </View>

        <SafeAreaView style={styles.safeArea}>
          <StatusBar style="light" />

          {/* Top Bar Header */}
          <View style={styles.header}>
            <View style={styles.brandRow}>
              <View style={styles.brandBadge}>
                <Text style={styles.brandBadgeText}>▶</Text>
              </View>
              <View>
                <Text style={styles.headerTitle}>LNXV</Text>
                <Text style={styles.headerSubtitle}>Liquid Glass Player</Text>
              </View>
            </View>

            <View style={styles.headerActions}>
              <TouchableOpacity style={styles.glassButton} onPress={cycleSpeed}>
                <Text style={styles.glassButtonText}>{playbackSpeed}</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.glassButton} onPress={cycleAspect}>
                <Text style={styles.glassButtonText}>{aspectRatioMode}</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* Video Player Section with Liquid Glass Frame */}
          <View style={styles.playerWrapper}>
            <View style={styles.videoContainer}>
              <VideoView
                style={styles.video}
                player={player}
                nativeControls={false}
                contentFit={contentFitValue}
              />

              {/* Floating Quick Controls Bar */}
              <View style={styles.videoOverlayControls}>
                <TouchableOpacity
                  style={styles.floatingCtrlBtn}
                  onPress={() => player.seekBy(-10)}
                >
                  <Text style={styles.ctrlIconText}>⏪ 10s</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={[styles.floatingCtrlBtn, styles.mainPlayBtn]}
                  onPress={() => (player.playing ? player.pause() : player.play())}
                >
                  <Text style={styles.mainPlayText}>
                    {player.playing ? '❚❚' : '▶'}
                  </Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={styles.floatingCtrlBtn}
                  onPress={() => player.seekBy(10)}
                >
                  <Text style={styles.ctrlIconText}>10s ⏩</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.floatingCtrlBtn} onPress={toggleMute}>
                  <Text style={styles.ctrlIconText}>{isMuted ? '🔇' : '🔊'}</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>

          {/* Active Media Meta Card */}
          <View style={styles.metaGlassCard}>
            <View style={styles.metaHeaderRow}>
              <Text style={styles.nowPlayingTag}>NOW PLAYING</Text>
              <View style={styles.qualityPill}>
                <Text style={styles.qualityText}>{selectedVideo.quality}</Text>
              </View>
            </View>
            <Text style={styles.currentTitle} numberOfLines={1}>
              {selectedVideo.title}
            </Text>
            <Text style={styles.currentDesc} numberOfLines={2}>
              {selectedVideo.description}
            </Text>
          </View>

          {/* Section Divider & Count */}
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Library Playlist</Text>
            <Text style={styles.sectionCount}>{SAMPLE_PLAYLIST.length} Videos</Text>
          </View>

          {/* Playlist Scroll Area */}
          <ScrollView
            style={styles.playlistList}
            contentContainerStyle={{ paddingBottom: 24 }}
            showsVerticalScrollIndicator={false}
          >
            {SAMPLE_PLAYLIST.map((item, index) => {
              const isSelected = item.id === selectedVideo.id;
              return (
                <TouchableOpacity
                  key={item.id}
                  activeOpacity={0.8}
                  style={[
                    styles.playlistCard,
                    isSelected && styles.activePlaylistCard,
                  ]}
                  onPress={() => handleSelectVideo(item)}
                >
                  {/* Thumbnail Mock */}
                  <View style={styles.thumbMock}>
                    <Text style={styles.thumbNumber}>0{index + 1}</Text>
                    <View style={styles.durationBadge}>
                      <Text style={styles.durationText}>{item.duration}</Text>
                    </View>
                  </View>

                  {/* Card Details */}
                  <View style={styles.cardDetails}>
                    <Text
                      style={[styles.cardTitle, isSelected && styles.activeCardTitle]}
                      numberOfLines={1}
                    >
                      {item.title}
                    </Text>
                    <Text style={styles.cardCategory}>{item.category}</Text>
                    <View style={styles.badgeRow}>
                      <Text style={styles.miniBadge}>{item.size}</Text>
                      <Text style={styles.miniBadge}>{item.quality}</Text>
                    </View>
                  </View>

                  {/* Play Status Icon */}
                  <View style={styles.statusCol}>
                    <Text style={isSelected ? styles.activePlayIcon : styles.idlePlayIcon}>
                      {isSelected ? '● Playing' : '▶'}
                    </Text>
                  </View>
                </TouchableOpacity>
              );
            })}
          </ScrollView>

          {/* Bottom Glass Navigation Bar */}
          <View style={styles.bottomNav}>
            <TouchableOpacity
              style={styles.navItem}
              onPress={() => setActiveTab('videos')}
            >
              <Text style={[styles.navIcon, activeTab === 'videos' && styles.activeNavIcon]}>🎬</Text>
              <Text style={[styles.navLabel, activeTab === 'videos' && styles.activeNavLabel]}>Videos</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.navItem}
              onPress={() => setActiveTab('folders')}
            >
              <Text style={[styles.navIcon, activeTab === 'folders' && styles.activeNavIcon]}>📁</Text>
              <Text style={[styles.navLabel, activeTab === 'folders' && styles.activeNavLabel]}>Folders</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.navItem}
              onPress={() => setActiveTab('vault')}
            >
              <Text style={[styles.navIcon, activeTab === 'vault' && styles.activeNavIcon]}>🔒</Text>
              <Text style={[styles.navLabel, activeTab === 'vault' && styles.activeNavLabel]}>Vault</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.navItem}
              onPress={() => setActiveTab('settings')}
            >
              <Text style={[styles.navIcon, activeTab === 'settings' && styles.activeNavIcon]}>⚙️</Text>
              <Text style={[styles.navLabel, activeTab === 'settings' && styles.activeNavLabel]}>Settings</Text>
            </TouchableOpacity>
          </View>
        </SafeAreaView>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  outerContainer: {
    flex: 1,
    backgroundColor: '#05070D',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Platform.OS === 'web' ? 20 : 0,
    minHeight: '100%',
  },
  phoneFrame: {
    width: Platform.OS === 'web' ? 420 : '100%',
    maxWidth: '100%',
    height: Platform.OS === 'web' ? 880 : '100%',
    backgroundColor: '#090D16',
    borderRadius: Platform.OS === 'web' ? 42 : 0,
    borderWidth: Platform.OS === 'web' ? 8 : 0,
    borderColor: '#1E293B',
    overflow: 'hidden',
    shadowColor: '#6366F1',
    shadowOffset: { width: 0, height: 16 },
    shadowOpacity: 0.35,
    shadowRadius: 36,
  },
  notchContainer: {
    width: '100%',
    alignItems: 'center',
    paddingTop: 8,
    backgroundColor: '#090D16',
  },
  dynamicIsland: {
    width: 110,
    height: 24,
    backgroundColor: '#000000',
    borderRadius: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
  },
  cameraHole: {
    width: 9,
    height: 9,
    borderRadius: 5,
    backgroundColor: '#1E293B',
  },
  speakerPill: {
    width: 44,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#1E293B',
  },
  safeArea: {
    flex: 1,
    backgroundColor: '#090D16',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 18,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.06)',
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  brandBadge: {
    width: 32,
    height: 32,
    borderRadius: 10,
    backgroundColor: '#6366F1',
    alignItems: 'center',
    justifyContent: 'center',
  },
  brandBadgeText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
  headerTitle: {
    fontSize: 17,
    fontWeight: '800',
    color: '#F8FAFC',
    letterSpacing: 1,
  },
  headerSubtitle: {
    fontSize: 11,
    color: '#818CF8',
    fontWeight: '500',
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  glassButton: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 14,
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  glassButtonText: {
    color: '#E2E8F0',
    fontSize: 11,
    fontWeight: '700',
  },
  playerWrapper: {
    paddingHorizontal: 12,
    paddingTop: 10,
  },
  videoContainer: {
    width: '100%',
    aspectRatio: 16 / 9,
    backgroundColor: '#000000',
    borderRadius: 16,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(99,102,241,0.25)',
    position: 'relative',
  },
  video: {
    width: '100%',
    height: '100%',
  },
  videoOverlayControls: {
    position: 'absolute',
    bottom: 8,
    left: 8,
    right: 8,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 12,
    backgroundColor: 'rgba(0,0,0,0.65)',
  },
  floatingCtrlBtn: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    backgroundColor: 'rgba(255,255,255,0.12)',
  },
  mainPlayBtn: {
    backgroundColor: '#6366F1',
    paddingHorizontal: 16,
    paddingVertical: 6,
  },
  mainPlayText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
  ctrlIconText: {
    color: '#F1F5F9',
    fontSize: 11,
    fontWeight: '600',
  },
  metaGlassCard: {
    marginHorizontal: 12,
    marginTop: 10,
    padding: 12,
    borderRadius: 14,
    backgroundColor: 'rgba(255,255,255,0.04)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  metaHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  nowPlayingTag: {
    fontSize: 10,
    fontWeight: '800',
    color: '#6366F1',
    letterSpacing: 1,
  },
  qualityPill: {
    backgroundColor: 'rgba(99,102,241,0.2)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
  },
  qualityText: {
    color: '#A5B4FC',
    fontSize: 10,
    fontWeight: '700',
  },
  currentTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#F8FAFC',
    marginBottom: 4,
  },
  currentDesc: {
    fontSize: 12,
    color: '#94A3B8',
    lineHeight: 16,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 18,
    paddingTop: 12,
    paddingBottom: 6,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#CBD5E1',
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  sectionCount: {
    fontSize: 11,
    color: '#64748B',
  },
  playlistList: {
    flex: 1,
    paddingHorizontal: 12,
  },
  playlistCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.03)',
    padding: 10,
    borderRadius: 14,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
  },
  activePlaylistCard: {
    backgroundColor: 'rgba(99,102,241,0.14)',
    borderColor: '#6366F1',
  },
  thumbMock: {
    width: 64,
    height: 48,
    borderRadius: 8,
    backgroundColor: '#1E293B',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
    position: 'relative',
  },
  thumbNumber: {
    fontSize: 16,
    fontWeight: '800',
    color: '#475569',
  },
  durationBadge: {
    position: 'absolute',
    bottom: 3,
    right: 4,
    backgroundColor: 'rgba(0,0,0,0.7)',
    paddingHorizontal: 4,
    borderRadius: 4,
  },
  durationText: {
    color: '#F8FAFC',
    fontSize: 8,
    fontWeight: '700',
  },
  cardDetails: {
    flex: 1,
  },
  cardTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#F1F5F9',
    marginBottom: 2,
  },
  activeCardTitle: {
    color: '#A5B4FC',
    fontWeight: '700',
  },
  cardCategory: {
    fontSize: 11,
    color: '#64748B',
    marginBottom: 4,
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 6,
  },
  miniBadge: {
    fontSize: 9,
    color: '#94A3B8',
    backgroundColor: 'rgba(255,255,255,0.06)',
    paddingHorizontal: 5,
    paddingVertical: 1,
    borderRadius: 4,
  },
  statusCol: {
    marginLeft: 8,
  },
  activePlayIcon: {
    color: '#6366F1',
    fontWeight: '700',
    fontSize: 11,
  },
  idlePlayIcon: {
    color: '#64748B',
    fontSize: 12,
  },
  bottomNav: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255,255,255,0.06)',
    backgroundColor: 'rgba(15,23,42,0.85)',
  },
  navItem: {
    alignItems: 'center',
    paddingHorizontal: 12,
  },
  navIcon: {
    fontSize: 16,
    opacity: 0.6,
  },
  activeNavIcon: {
    opacity: 1,
  },
  navLabel: {
    fontSize: 10,
    color: '#64748B',
    marginTop: 2,
    fontWeight: '600',
  },
  activeNavLabel: {
    color: '#818CF8',
    fontWeight: '700',
  },
});
