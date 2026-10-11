using Microsoft.EntityFrameworkCore;
using TendApi.Models;

namespace TendApi.Data;

public class TendDbContext : DbContext
{
    public TendDbContext(DbContextOptions<TendDbContext> options) : base(options) { }

    public DbSet<User> Users => Set<User>();
    public DbSet<Habit> Habits => Set<Habit>();
    public DbSet<CheckIn> CheckIns => Set<CheckIn>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(e =>
        {
            e.HasIndex(u => u.Email).IsUnique();
            e.Property(u => u.Email).HasMaxLength(256);
            e.Property(u => u.DisplayName).HasMaxLength(60);
            e.Property(u => u.WeekStartsOn).HasMaxLength(10);
            e.Property(u => u.Colour).HasMaxLength(20);
            e.Property(u => u.PhotoContentType).HasMaxLength(50);
        });

        modelBuilder.Entity<Habit>(e =>
        {
            e.Property(h => h.Name).HasMaxLength(60);
            e.Property(h => h.Emoji).HasMaxLength(16);
            e.HasOne(h => h.User)
                .WithMany(u => u.Habits)
                .HasForeignKey(h => h.UserId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<CheckIn>(e =>
        {
            e.HasIndex(c => new { c.HabitId, c.Date }).IsUnique();
            e.HasOne(c => c.Habit)
                .WithMany(h => h.CheckIns)
                .HasForeignKey(c => c.HabitId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }
}
