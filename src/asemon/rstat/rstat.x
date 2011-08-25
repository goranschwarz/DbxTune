/*
 * Sun RPC is a product of Sun Microsystems, Inc. and is provided for
 * unrestricted use provided that this legend is included on all tape
 * media and as a part of the software program in whole or part.  Users
 * may copy or modify Sun RPC without charge, but are not authorized
 * to license or distribute it to anyone else except as part of a product or
 * program developed by the user.
 * 
 * SUN RPC IS PROVIDED AS IS WITH NO WARRANTIES OF ANY KIND INCLUDING THE
 * WARRANTIES OF DESIGN, MERCHANTIBILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE, OR ARISING FROM A COURSE OF DEALING, USAGE OR TRADE PRACTICE.
 * 
 * Sun RPC is provided with no support and without any obligation on the
 * part of Sun Microsystems, Inc. to assist in its use, correction,
 * modification or enhancement.
 * 
 * SUN MICROSYSTEMS, INC. SHALL HAVE NO LIABILITY WITH RESPECT TO THE
 * INFRINGEMENT OF COPYRIGHTS, TRADE SECRETS OR ANY PATENTS BY SUN RPC
 * OR ANY PART THEREOF.
 * 
 * In no event will Sun Microsystems, Inc. be liable for any lost revenue
 * or profits or other special, indirect and consequential damages, even if
 * Sun has been advised of the possibility of such damages.
 * 
 * Sun Microsystems, Inc.
 * 2550 Garcia Avenue
 * Mountain View, California  94043
 *
 * Modified by Adam Migus (amigus@cs.mun.ca)  
 */

/*
 * Gather statistics on remote machines
 */

const CPUSTATES = 4;
const DK_NDRIVE = 4;

struct rstat_timeval {
	unsigned int tv_sec;		/* seconds */
	unsigned int tv_usec;	/* and microseconds */
};

struct statsusers {				/* RSTATVERS_USERS */
	int cp_time[CPUSTATES];
	int dk_xfer[DK_NDRIVE];
	unsigned int v_pgpgin;	/* these are cumulative sum */
	unsigned int v_pgpgout;
	unsigned int v_pswpin;
	unsigned int v_pswpout;
	unsigned int v_intr;
	int if_ipackets;
	int if_ierrors;
	int if_oerrors;
	int if_collisions;
	unsigned int v_swtch;
	int avenrun[3];         /* scaled by FSCALE */
	rstat_timeval boottime;
	rstat_timeval curtime;
	int if_opackets;
	unsigned int users;
};

struct statsvar {				/* RSTATVERS_VAR */
	int cp_time[CPUSTATES]; /* variable number of CPU states */
        int dk_xfer[DK_NDRIVE]; /* variable number of disks */
	unsigned v_pgpgin;      /* these are cumulative sum */
	unsigned v_pgpgout;
	unsigned v_pswpin;
	unsigned v_pswpout;
	unsigned v_intr;
	int if_ipackets;
	int if_ierrors;
	int if_opackets;
	int if_oerrors;
	int if_collisions;
	unsigned v_swtch;
	long avenrun[3];
	rstat_timeval boottime;
	rstat_timeval curtime;
};


struct statstime {				/* RSTATVERS_TIME */
	int cp_time[CPUSTATES];
	int dk_xfer[DK_NDRIVE];
	unsigned int v_pgpgin;	/* these are cumulative sum */
	unsigned int v_pgpgout;
	unsigned int v_pswpin;
	unsigned int v_pswpout;
	unsigned int v_intr;
	int if_ipackets;
	int if_ierrors;
	int if_oerrors;
	int if_collisions;
	unsigned int v_swtch;
	int avenrun[3];         /* scaled by FSCALE */
	rstat_timeval boottime;
	rstat_timeval curtime;
	int if_opackets;
};

struct statsswtch {			/* RSTATVERS_SWTCH */
	int cp_time[CPUSTATES];
	int dk_xfer[DK_NDRIVE];
	unsigned int v_pgpgin;	/* these are cumulative sum */
	unsigned int v_pgpgout;
	unsigned int v_pswpin;
	unsigned int v_pswpout;
	unsigned int v_intr;
	int if_ipackets;
	int if_ierrors;
	int if_oerrors;
	int if_collisions;
	unsigned int v_swtch;
	unsigned int avenrun[3];/* scaled by FSCALE */
	rstat_timeval boottime;
	int if_opackets;
};

struct stats {				/* RSTATVERS_ORIG */
	int cp_time[CPUSTATES];
	int dk_xfer[DK_NDRIVE];
	unsigned int v_pgpgin;	/* these are cumulative sum */
	unsigned int v_pgpgout;
	unsigned int v_pswpin;
	unsigned int v_pswpout;
	unsigned int v_intr;
	int if_ipackets;
	int if_ierrors;
	int if_oerrors;
	int if_collisions;
	int if_opackets;
};


program RSTATPROG {
	/*
	 * This version includes users info
	 */
	version RSTATVERS_USERS {
		statsusers
		RSTATPROC_STATS(void) = 1;

		unsigned int
		RSTATPROC_HAVEDISK(void) = 2;
	} = 5;
        /*
         * Version 4 allows for variable number of disk and RSTAT_CPU states.
         */
        version RSTATVERS_VAR {
                statsvar
                RSTATPROC_STATS (void) = 1;

                unsigned int
                RSTATPROC_HAVEDISK (void) = 2;
        } = 4;
	/*
	 * Second Newest version includes current time and context switching info
	 */
	version RSTATVERS_TIME {
		statstime
		RSTATPROC_STATS(void) = 1;

		unsigned int
		RSTATPROC_HAVEDISK(void) = 2;
	} = 3;
	/*
	 * Does not have current time
	 */
	version RSTATVERS_SWTCH {
		statsswtch
		RSTATPROC_STATS(void) = 1;

		unsigned int
		RSTATPROC_HAVEDISK(void) = 2;
	} = 2;
	/*
	 * Old version has no info about current time or context switching
	 */
	version RSTATVERS_ORIG {
		stats
		RSTATPROC_STATS(void) = 1;

		unsigned int
		RSTATPROC_HAVEDISK(void) = 2;
	} = 1;
} = 100001;
